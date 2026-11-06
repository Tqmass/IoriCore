package us.iorilabs.proxy.staffauth;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import us.iorilabs.api.messaging.PluginMessageType;
import us.iorilabs.api.redis.NetworkEventKeys;
import us.iorilabs.proxy.config.StaffAuthConfig;
import us.iorilabs.proxy.database.StorageService;
import us.iorilabs.proxy.language.LanguageManager;
import us.iorilabs.proxy.logging.ProxyLogger;
import us.iorilabs.proxy.platform.ProxyPlatform;
import us.iorilabs.proxy.player.ProxyPlayer;
import us.iorilabs.proxy.redis.ProxyEventType;
import us.iorilabs.proxy.redis.RedisEvent;
import us.iorilabs.proxy.redis.RedisService;
import us.iorilabs.proxy.staff.StaffGroups;
import us.iorilabs.proxy.staffauth.enums.StaffAuthState;
import us.iorilabs.proxy.webhooks.WebhookRegistry;

public final class StaffAuthService {

    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    private final ProxyPlatform platform;
    private final ProxyLogger logger;
    private final LanguageManager language;
    private final WebhookRegistry webhooks;
    private final StaffGroups staffGroups;
    private final StaffSessionSql sessionSql;
    private final RedisService redis;
    private final String proxyId;
    private StaffAuthConfig config;

    private final Map<UUID, LastSeen> lastSeen = new ConcurrentHashMap<>();
    private final Set<UUID> restored = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();
    private final Map<String, UUID> tokens = new ConcurrentHashMap<>();
    private volatile StaffAuthDiscordSink discordSink;

    public StaffAuthService(
        ProxyPlatform platform,
        ProxyLogger logger,
        LanguageManager language,
        WebhookRegistry webhooks,
        StaffGroups staffGroups,
        StaffAuthConfig config,
        StorageService storage
    ) {
        this(platform, logger, language, webhooks, staffGroups, config, storage, null, "");
    }

    public StaffAuthService(
        ProxyPlatform platform,
        ProxyLogger logger,
        LanguageManager language,
        WebhookRegistry webhooks,
        StaffGroups staffGroups,
        StaffAuthConfig config,
        StorageService storage,
        RedisService redis,
        String proxyId
    ) {
        this.platform = platform;
        this.logger = logger;
        this.language = language;
        this.webhooks = webhooks;
        this.staffGroups = staffGroups;
        this.config = config;
        this.sessionSql = new StaffSessionSql(storage, logger);
        this.redis = redis;
        this.proxyId = proxyId == null ? "" : proxyId;
        if (redis != null) {
            redis.subscribe(ProxyEventType.STAFF_AUTH_RESPONSE.id(), this::onAuthResponse);
        }
    }

    public void reload(StaffAuthConfig config) {
        if (config != null) this.config = config;
    }

    public StaffAuthConfig config() {
        return config;
    }

    public boolean isStaff(ProxyPlayer player) {
        if (player == null) return false;
        if (staffGroups != null && staffGroups.isStaff(player)) return true;
        String fallback = config.staffPermissionFallback();
        return fallback != null && !fallback.isBlank() && player.hasPermission(fallback);
    }

    public void onStaffJoin(ProxyPlayer player, String ip) {
        if (player == null || !isStaff(player)) return;

        String normalizedIp = normalizeIp(ip);
        if (!lastSeen.containsKey(player.uuid()) && !restored.contains(player.uuid()) && sessionSql.available()) {
            platform.asyncExecutor().execute(() -> restoreAndEvaluate(player, normalizedIp));
            return;
        }

        evaluateJoin(player, normalizedIp);
    }

    private void restoreAndEvaluate(ProxyPlayer player, String normalizedIp) {
        UUID uuid = player.uuid();
        try {
            sessionSql
                .load(uuid)
                .ifPresent(stored -> lastSeen.putIfAbsent(uuid, new LastSeen(stored.ip(), safe(stored.country()))));
            restored.add(uuid);
            evaluateJoin(player, normalizedIp);
        } catch (Exception ex) {
            logger.warn("StaffAuth session restore failed (challenge): " + ex.getMessage());
            beginPending(player, normalizedIp, null);
        }

        publishSession(uuid);
    }

    private void remember(UUID uuid, LastSeen seen) {
        lastSeen.put(uuid, seen);
        platform.asyncExecutor().execute(() -> sessionSql.save(uuid, seen.ip, seen.country));
    }

    private void evaluateJoin(ProxyPlayer player, String normalizedIp) {
        LastSeen previous = lastSeen.get(player.uuid());
        Session existing = sessions.get(player.uuid());

        if (existing != null && existing.state == StaffAuthState.PENDING && sameIp(existing.currentIp, normalizedIp)) {
            notifyPlayerPending(player, existing);
            return;
        }

        if (previous == null || previous.ip == null || previous.ip.isBlank()) {
            sessions.put(player.uuid(), Session.approved(normalizedIp, "?"));
            remember(player.uuid(), new LastSeen(normalizedIp, "?"));
            publishSession(player.uuid());
            if (config.geoLookup()) {
                platform.asyncExecutor().execute(() -> baselineCountry(player.uuid(), normalizedIp));
            }
            return;
        }

        if (sameIp(previous.ip, normalizedIp)) {
            sessions.put(player.uuid(), Session.approved(normalizedIp, previous.country));
            publishSession(player.uuid());
            if (config.geoLookup()) {
                platform.asyncExecutor().execute(() -> verifyGeoOrChallenge(player, normalizedIp, previous));
            }
            return;
        }

        beginPending(player, normalizedIp, previous);
    }

    public void onQuit(UUID uuid) {
        if (uuid == null) return;
        Session session = sessions.get(uuid);
        if (session != null && session.state == StaffAuthState.APPROVED) {
            sessions.remove(uuid);
        }
    }

    public boolean isAuthenticated(UUID uuid) {
        if (uuid == null) return false;
        Session session = sessions.get(uuid);
        return session != null && session.state == StaffAuthState.APPROVED && !session.expired();
    }

    public boolean isPending(UUID uuid) {
        if (uuid == null) return false;
        Session session = sessions.get(uuid);
        return session != null && session.state == StaffAuthState.PENDING && !session.expired();
    }

    public boolean ownsToken(String token, UUID uuid) {
        if (token == null || token.isBlank() || uuid == null) return false;

        return uuid.equals(tokens.get(token.trim().toLowerCase(Locale.ROOT)));
    }

    public Optional<String> approve(String token, String actor) {
        return approve(token, actor, null);
    }

    public Optional<String> approve(String token, String actor, UUID actorUuid) {
        if (token == null || token.isBlank()) return Optional.empty();

        String normalized = token.trim().toLowerCase(Locale.ROOT);
        UUID target = tokens.get(normalized);
        if (target == null || target.equals(actorUuid)) return Optional.empty();
        if (!tokens.remove(normalized, target)) return Optional.empty();

        UUID uuid = target;

        Session pending = sessions.get(uuid);
        if (pending == null || pending.state != StaffAuthState.PENDING) return Optional.empty();

        String country =
            pending.currentCountry == null || pending.currentCountry.isBlank() ? "?" : pending.currentCountry;
        remember(uuid, new LastSeen(pending.currentIp, country));
        sessions.put(uuid, Session.approved(pending.currentIp, country));

        platform
            .findPlayerByUuid(uuid)
            .ifPresent(player ->
                player.sendMessage(
                    language.rawFor(player, "staff-auth.approved", Map.of("actor", actor == null ? "?" : actor))
                )
            );

        publishSession(uuid);
        logger.module("StaffAuth", "approved &f" + uuid + " &7by &f" + (actor == null ? "?" : actor));
        return Optional.of(uuid.toString());
    }

    public Optional<String> deny(String token, String actor) {
        if (token == null || token.isBlank()) return Optional.empty();

        String normalized = token.trim().toLowerCase(Locale.ROOT);
        UUID target = tokens.remove(normalized);
        if (target == null) return Optional.empty();

        sessions.remove(target);
        String message = language.raw("staff-auth.denied", Map.of("actor", actor == null ? "Discord" : actor));
        platform.kickPlayer(target, message == null || message.isBlank() ? "&cStaff auth denied." : message);
        logger.module("StaffAuth", "denied &f" + target + " &7by &f" + (actor == null ? "?" : actor));
        return Optional.of(target.toString());
    }

    private void onAuthResponse(RedisEvent event) {
        if (event == null || event.data() == null) return;

        JsonObject data = event.data();
        if (!data.has(NetworkEventKeys.TOKEN) || !data.has(NetworkEventKeys.APPROVED)) return;

        String token = data.get(NetworkEventKeys.TOKEN).getAsString();
        boolean approved = data.get(NetworkEventKeys.APPROVED).getAsBoolean();
        String actor = data.has(NetworkEventKeys.ACTOR) ? data.get(NetworkEventKeys.ACTOR).getAsString() : "Discord";
        UUID actorUuid = null;
        if (data.has("actor_uuid") && !data.get("actor_uuid").isJsonNull()) {
            try {
                actorUuid = UUID.fromString(data.get("actor_uuid").getAsString());
            } catch (IllegalArgumentException ignored) {}
        }

        if (approved) {
            approve(token, actor, actorUuid);
        } else {
            deny(token, actor);
        }
    }

    public byte[] encodeSessionPayload(UUID uuid) {
        boolean ok = isAuthenticated(uuid);
        return new byte[] { PluginMessageType.STAFF_AUTH_SESSION.id(), (byte) (ok ? 1 : 0) };
    }

    private void publishSession(UUID uuid) {
        if (redis == null || uuid == null) return;

        JsonObject data = new JsonObject();
        data.addProperty(NetworkEventKeys.UUID, uuid.toString());
        data.addProperty(NetworkEventKeys.APPROVED, isAuthenticated(uuid));
        redis.publish(new RedisEvent(ProxyEventType.STAFF_AUTH_SESSION.id(), proxyId, data));
    }

    public static String maskIp(String ip) {
        if (ip == null || ip.isBlank()) return "?";
        String value = ip.trim();
        int lastDot = value.lastIndexOf('.');
        if (lastDot > 0) return value.substring(0, lastDot + 1) + "x";
        int colon = value.indexOf(':');
        if (colon > 0) return value.substring(0, Math.min(value.length(), colon + 3)) + "…";
        return value;
    }

    private void beginPending(ProxyPlayer player, String ip, LastSeen previous) {
        Session prior = sessions.get(player.uuid());
        if (prior != null && prior.token != null) {
            tokens.remove(prior.token);
        }

        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toLowerCase(Locale.ROOT);
        long expiresAt = System.currentTimeMillis() + config.tokenTtlSeconds() * 1000L;
        String previousCountry = previous == null ? "?" : previous.country;
        Session pending = Session.pending(ip, previousCountry, token, expiresAt);
        sessions.put(player.uuid(), pending);
        tokens.put(token, player.uuid());

        notifyPlayerPending(player, pending);
        publishSession(player.uuid());
        logger.module("StaffAuth", "pending approval for &f" + player.name());
        requestCountryAndNotify(player, ip, previousCountry, token);
    }

    private void verifyGeoOrChallenge(ProxyPlayer player, String ip, LastSeen previous) {
        String country = lookupCountry(ip);
        if (!countryChanged(previous.country, country)) {
            remember(player.uuid(), new LastSeen(ip, country.equals("?") ? previous.country : country));
            return;
        }

        Session session = sessions.get(player.uuid());
        if (session == null || session.state != StaffAuthState.APPROVED) return;
        if (!sameIp(session.currentIp, ip)) return;

        beginPending(player, ip, previous);
    }

    private void baselineCountry(UUID uuid, String ip) {
        String country = lookupCountry(ip);
        LastSeen seen = lastSeen.get(uuid);
        if (seen == null || !sameIp(seen.ip, ip)) return;
        remember(uuid, new LastSeen(ip, country));
        Session session = sessions.get(uuid);
        if (session != null && session.state == StaffAuthState.APPROVED && sameIp(session.currentIp, ip)) {
            sessions.put(uuid, Session.approved(ip, country));
        }
    }

    private void notifyPlayerPending(ProxyPlayer player, Session pending) {
        Map<String, String> ph = Map.of(
            "ip",
            config.maskIp() ? maskIp(pending.currentIp) : safe(pending.currentIp),
            "country",
            safe(pending.currentCountry)
        );
        for (String line : language.rawList("staff-auth.pending", ph, player.protocolVersion())) {
            player.sendMessage(line);
        }
    }

    private void requestCountryAndNotify(ProxyPlayer player, String ip, String previousCountry, String token) {
        if (!config.geoLookup()) {
            fireDiscord(player, ip, previousCountry, "?", token);
            return;
        }

        platform.asyncExecutor().execute(() -> {
            String country = lookupCountry(ip);
            Session session = sessions.get(player.uuid());
            if (session != null && session.state == StaffAuthState.PENDING && sameIp(session.currentIp, ip)) {
                sessions.put(player.uuid(), Session.pending(ip, country, session.token, session.expiresAtMillis));
            }
            fireDiscord(player, ip, previousCountry, country, token);
        });
    }

    private void fireDiscord(ProxyPlayer player, String ip, String previousCountry, String country, String token) {
        String displayIp = config.maskIp() ? maskIp(ip) : safe(ip);
        Map<String, String> placeholders = Map.of(
            "player",
            player.name(),
            "uuid",
            player.uuid().toString(),
            "ip",
            displayIp,
            "country",
            safe(country),
            "previous_country",
            safe(previousCountry),
            "token",
            safe(token)
        );

        publishAuthRequest(player, displayIp, previousCountry, country, token);

        if (config.webhookEnable() && webhooks != null) {
            webhooks.send("staff-auth", placeholders);
        }

        StaffAuthDiscordSink sink = discordSink;
        if (sink != null) {
            try {
                sink.accept(placeholders);
            } catch (Exception ignored) {}
        }

        logger.module(
            "StaffAuth",
            "pending discord · " + player.name() + " ip=" + displayIp + " country=" + safe(country)
        );
    }

    private void publishAuthRequest(
        ProxyPlayer player,
        String displayIp,
        String previousCountry,
        String country,
        String token
    ) {
        if (redis == null || player == null) return;

        JsonObject data = new JsonObject();
        data.addProperty(NetworkEventKeys.UUID, player.uuid().toString());
        data.addProperty(NetworkEventKeys.PLAYER, player.name());
        data.addProperty(NetworkEventKeys.TOKEN, safe(token));
        data.addProperty(NetworkEventKeys.IP, displayIp);
        data.addProperty(NetworkEventKeys.COUNTRY, safe(country));
        data.addProperty("previous_country", safe(previousCountry));
        redis.publish(new RedisEvent(ProxyEventType.STAFF_AUTH_REQUEST.id(), proxyId, data));
    }

    public void discordSink(StaffAuthDiscordSink sink) {
        this.discordSink = sink;
    }

    public Optional<UUID> pendingTargetUuid(String token) {
        if (token == null || token.isBlank()) return Optional.empty();

        String normalized = token.trim().toLowerCase(Locale.ROOT);
        return Optional.ofNullable(tokens.get(normalized));
    }

    public Optional<Long> pendingExpiresAtMillis(String token) {
        if (token == null || token.isBlank()) return Optional.empty();

        String normalized = token.trim().toLowerCase(Locale.ROOT);
        UUID uuid = tokens.get(normalized);
        if (uuid == null) return Optional.empty();

        Session session = sessions.get(uuid);
        if (session == null || session.state != StaffAuthState.PENDING) return Optional.empty();

        return Optional.of(session.expiresAtMillis);
    }

    @FunctionalInterface
    public interface StaffAuthDiscordSink {
        void accept(Map<String, String> placeholders);
    }

    private String lookupCountry(String ip) {
        if (ip == null || ip.isBlank() || ip.startsWith("127.") || "0.0.0.0".equals(ip)) return "?";

        try {
            String url = config.geoUrlTemplate().replace("%ip%", ip).replace("{ip}", ip);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) return "?";

            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            if (json.has("status") && !"success".equalsIgnoreCase(json.get("status").getAsString())) return "?";
            if (json.has("countryCode") && !json.get("countryCode").getAsString().isBlank()) {
                return json.get("countryCode").getAsString();
            }
            if (json.has("country") && !json.get("country").getAsString().isBlank()) {
                return json.get("country").getAsString();
            }
        } catch (Exception ex) {
            logger.warn("StaffAuth geo lookup failed: " + ex.getMessage());
        }
        return "?";
    }

    private static boolean countryChanged(String previous, String current) {
        if (previous == null || previous.isBlank() || "?".equals(previous)) return false;
        if (current == null || current.isBlank() || "?".equals(current)) return false;
        return !previous.equalsIgnoreCase(current);
    }

    private static boolean sameIp(String a, String b) {
        return normalizeIp(a).equals(normalizeIp(b));
    }

    private static String normalizeIp(String ip) {
        if (ip == null) return "";
        return ip.trim().toLowerCase(Locale.ROOT);
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "?" : value;
    }

    private static final class LastSeen {

        private final String ip;
        private final String country;

        private LastSeen(String ip, String country) {
            this.ip = ip;
            this.country = country;
        }
    }

    private static final class Session {

        private final StaffAuthState state;
        private final String currentIp;
        private final String currentCountry;
        private final String token;
        private final long expiresAtMillis;

        private Session(
            StaffAuthState state,
            String currentIp,
            String currentCountry,
            String token,
            long expiresAtMillis
        ) {
            this.state = state;
            this.currentIp = currentIp;
            this.currentCountry = currentCountry;
            this.token = token;
            this.expiresAtMillis = expiresAtMillis;
        }

        private static Session approved(String ip, String country) {
            return new Session(StaffAuthState.APPROVED, ip, country, null, Long.MAX_VALUE);
        }

        private static Session pending(String ip, String country, String token, long expiresAt) {
            return new Session(StaffAuthState.PENDING, ip, country, token, expiresAt);
        }

        private boolean expired() {
            return state == StaffAuthState.PENDING && System.currentTimeMillis() > expiresAtMillis;
        }
    }
}
