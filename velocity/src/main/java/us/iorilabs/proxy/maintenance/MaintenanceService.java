package us.iorilabs.proxy.maintenance;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.experimental.Accessors;
import us.iorilabs.proxy.config.MaintenanceConfig;
import us.iorilabs.proxy.language.LanguageManager;
import us.iorilabs.proxy.logging.ProxyLogger;
import us.iorilabs.proxy.platform.ProxyPlatform;
import us.iorilabs.proxy.player.ProxyPlayer;
import us.iorilabs.proxy.redis.ProxyEventType;
import us.iorilabs.proxy.redis.RedisEvent;
import us.iorilabs.proxy.redis.RedisService;
import us.iorilabs.proxy.server.ServerModes;
import us.iorilabs.proxy.utilities.DurationParser;

@Accessors(fluent = true)
public final class MaintenanceService {

    private final ProxyPlatform platform;
    private final RedisService redis;

    @Getter
    private MaintenanceConfig config;

    private final LanguageManager language;
    private final ProxyLogger logger;
    private final String proxyId;
    private final Map<String, MaintenanceState> states = new ConcurrentHashMap<>();
    private final Set<String> whitelist = ConcurrentHashMap.newKeySet();
    private final Set<UUID> countdownLeaders = ConcurrentHashMap.newKeySet();

    public MaintenanceService(
        ProxyPlatform platform,
        RedisService redis,
        MaintenanceConfig config,
        LanguageManager language,
        ProxyLogger logger,
        String proxyId
    ) {
        this.platform = platform;
        this.redis = redis;
        this.config = config;
        this.language = language;
        this.logger = logger;
        this.proxyId = proxyId;
        whitelist.addAll(normalizeAll(config.whitelist()));
        redis.subscribe(ProxyEventType.MAINTENANCE_UPDATE.id(), this::onRemoteState);
        redis.subscribe(ProxyEventType.MAINTENANCE_WHITELIST.id(), this::onRemoteWhitelist);
        loadFromRedis();
        platform.runTimer(this::tickExpirations, 1000L, 1000L, "maintenance-expiry");
    }

    public void reloadConfig(MaintenanceConfig config) {
        this.config = config;
        whitelist.clear();
        whitelist.addAll(normalizeAll(config.whitelist()));
    }

    public boolean isGlobalActive() {
        return find(MaintenanceType.GLOBAL, "global")
            .map(state -> state.isCurrentlyActive(System.currentTimeMillis()))
            .orElse(false);
    }

    public Map<String, List<String>> modes() {
        return ServerModes.resolve(platform, config.modes());
    }

    public boolean isServerBlocked(String serverName) {
        long now = System.currentTimeMillis();
        if (isGlobalActive()) return true;
        if (serverName == null || serverName.isBlank()) return false;

        Optional<MaintenanceState> server = find(MaintenanceType.SERVER, serverName);
        if (server.isPresent() && server.get().isCurrentlyActive(now)) return true;

        for (Map.Entry<String, List<String>> mode : modes().entrySet()) {
            if (
                !mode
                    .getValue()
                    .stream()
                    .anyMatch(s -> s.equalsIgnoreCase(serverName))
            ) {
                continue;
            }

            Optional<MaintenanceState> modeState = find(MaintenanceType.MODE, mode.getKey());
            if (modeState.isPresent() && modeState.get().isCurrentlyActive(now)) {
                // Lobby / allow-list stays open during mode maintenance.
                return !isAllowedDuringMaintenance(serverName);
            }
        }

        return false;
    }

    public boolean canBypass(ProxyPlayer player) {
        if (player.hasPermission(config.bypassPermission()) || player.hasPermission("ioriproxy.maintenance.*")) {
            return true;
        }

        return (
            whitelist.contains(player.name().toLowerCase(Locale.ROOT)) ||
            whitelist.contains(player.uuid().toString().toLowerCase(Locale.ROOT))
        );
    }

    public Optional<MaintenanceState> find(MaintenanceType scope, String id) {
        return Optional.ofNullable(states.get(key(scope, id)));
    }

    public List<MaintenanceState> activeStates() {
        long now = System.currentTimeMillis();
        List<MaintenanceState> active = new ArrayList<>();

        for (MaintenanceState state : states.values()) {
            if (state.isCurrentlyActive(now)) {
                active.add(state);
            }
        }

        return active;
    }

    public void enable(
        MaintenanceType scope,
        String id,
        String reason,
        String by,
        long durationMillis,
        long countdownMillis
    ) {
        long now = System.currentTimeMillis();
        boolean hasCountdown = countdownMillis > 0 && config.countdownEnabled();
        long startAt = hasCountdown ? now + countdownMillis : now;
        long endAt = durationMillis > 0 ? startAt + durationMillis : 0L;

        MaintenanceState state = new MaintenanceState(
            scope,
            id,
            true,
            reason == null || reason.isBlank() ? config.defaultReason() : reason,
            startAt,
            endAt,
            by
        );
        states.put(state.key(), state);
        persistState(state);
        publishState(state);

        if (hasCountdown) {
            startCountdown(state, countdownMillis, durationMillis);
        } else {
            onActivated(state);
        }

        alertStaff(true, displayScope(state), by);
    }

    public void enable(
        MaintenanceType scope,
        String id,
        String reason,
        String by,
        long durationMillis,
        boolean scheduleCountdown
    ) {
        if (scheduleCountdown) {
            enable(scope, id, reason, by, 0L, durationMillis);
        } else {
            enable(scope, id, reason, by, durationMillis, 0L);
        }
    }

    public void disable(MaintenanceType scope, String id, String by) {
        MaintenanceState previous = states.get(key(scope, id));
        MaintenanceState state = new MaintenanceState(
            scope,
            id,
            false,
            previous == null ? "" : previous.reason(),
            0,
            0,
            by
        );
        states.put(state.key(), state);
        persistState(state);
        publishState(state);
        platform.cancelTask("maintenance-countdown-" + state.key());
        broadcastScopedMessage(state, "maintenance.countdown.ended");
        logger.module("Maintenance", "disabled " + state.key() + " by " + by);
        alertStaff(false, displayScope(state), by);
    }

    private void alertStaff(boolean enabled, String scope, String by) {
        us.iorilabs.proxy.IoriProxy core = us.iorilabs.proxy.IoriProxy.get();
        if (core == null || core.staffAlerts() == null) return;

        core.staffAlerts().maintenance(enabled, scope, by);
    }

    public void addWhitelist(String entry) {
        String normalized = entry.toLowerCase(Locale.ROOT);
        whitelist.add(normalized);
        saveWhitelist();
        publishWhitelist();
    }

    public boolean removeWhitelist(String entry) {
        if (entry == null || entry.isBlank()) return false;

        String normalized = entry.trim().toLowerCase(Locale.ROOT);
        boolean removed = whitelist.remove(normalized);
        if (!removed) {
            // Also try matching display names that differ only by surrounding spaces.
            for (String existing : List.copyOf(whitelist)) {
                if (existing.equalsIgnoreCase(normalized)) {
                    removed = whitelist.remove(existing);
                    break;
                }
            }
        }

        if (!removed) return false;

        saveWhitelist();
        publishWhitelist();
        return true;
    }

    public Set<String> whitelistEntries() {
        return Set.copyOf(whitelist);
    }

    public void transferWhitelist(UUID from, String fromName, UUID to, String toName) {
        boolean changed = false;
        if (from != null && whitelist.remove(from.toString().toLowerCase(Locale.ROOT))) {
            whitelist.add(to.toString().toLowerCase(Locale.ROOT));
            changed = true;
        }

        if (fromName != null && whitelist.remove(fromName.toLowerCase(Locale.ROOT))) {
            if (toName != null && !toName.isBlank()) {
                whitelist.add(toName.toLowerCase(Locale.ROOT));
            } else {
                whitelist.add(to.toString().toLowerCase(Locale.ROOT));
            }
            changed = true;
        }

        if (changed) {
            saveWhitelist();
            publishWhitelist();
        }
    }

    public String kickMessage(MaintenanceState state) {
        return kickMessage(state, Integer.MAX_VALUE);
    }

    public String kickMessage(MaintenanceState state, int protocol) {
        boolean legacy = protocol <= language.legacyProtocolMax();
        String configured = legacy ? config.kickDescriptionLegacy() : config.kickDescription();
        return text(configured, "maintenance.kick", Map.of("reason", state.reason(), "scope", displayScope(state)));
    }

    public Optional<MaintenanceState> blockingState(String serverName) {
        long now = System.currentTimeMillis();
        Optional<MaintenanceState> global = find(MaintenanceType.GLOBAL, "global");
        if (global.isPresent() && global.get().isCurrentlyActive(now)) return global;

        if (serverName != null) {
            Optional<MaintenanceState> server = find(MaintenanceType.SERVER, serverName);
            if (server.isPresent() && server.get().isCurrentlyActive(now)) return server;

            for (Map.Entry<String, List<String>> mode : modes().entrySet()) {
                if (
                    mode
                        .getValue()
                        .stream()
                        .anyMatch(s -> s.equalsIgnoreCase(serverName))
                ) {
                    Optional<MaintenanceState> modeState = find(MaintenanceType.MODE, mode.getKey());
                    if (modeState.isPresent() && modeState.get().isCurrentlyActive(now)) return modeState;
                }
            }
        }

        return Optional.empty();
    }

    private void startCountdown(MaintenanceState state, long warnMillis, long durationMillis) {
        if (warnMillis <= 0) {
            onActivated(state);
            return;
        }

        String taskId = "maintenance-countdown-" + state.key();
        long started = System.currentTimeMillis();
        Set<Integer> announced = ConcurrentHashMap.newKeySet();
        platform.runTimer(
            () -> {
                long elapsed = System.currentTimeMillis() - started;
                long remaining = warnMillis - elapsed;
                if (remaining <= 0) {
                    platform.cancelTask(taskId);
                    long now = System.currentTimeMillis();
                    long endAt = durationMillis > 0 ? now + durationMillis : 0L;
                    MaintenanceState active = new MaintenanceState(
                        state.scopeType(),
                        state.scopeId(),
                        true,
                        state.reason(),
                        now,
                        endAt,
                        state.enabledBy()
                    );
                    states.put(active.key(), active);
                    persistState(active);
                    publishState(active);
                    onActivated(active);
                    return;
                }

                int seconds = (int) Math.ceil(remaining / 1000.0);
                if (seconds <= 0) return;

                for (Integer interval : config.countdownIntervals()) {
                    if (seconds == interval && announced.add(interval)) {
                        Map<String, String> placeholders = scopePlaceholders(state, remaining);
                        for (ProxyPlayer player : scopedPlayers(state)) {
                            int protocol = player.protocolVersion();
                            String chat = language.raw("maintenance.countdown.chat", placeholders, protocol);
                            player.sendMessage(chat);

                            if (config.title()) {
                                platform.sendTitle(
                                    player.uuid(),
                                    language.raw("maintenance.countdown.title_main", placeholders, protocol),
                                    language.raw("maintenance.countdown.title_sub", placeholders, protocol)
                                );
                            }

                            if (config.actionbar()) {
                                platform.sendActionBar(player.uuid(), chat);
                            }
                        }
                    }
                }
            },
            0L,
            1000L,
            taskId
        );
    }

    private void onActivated(MaintenanceState state) {
        broadcastScopedMessage(state, "maintenance.countdown.started");

        if (config.kickNonWhitelisted()) {
            kickAffected(state);
        }

        logger.module("Maintenance", "active " + state.key());
    }

    private void broadcastScopedMessage(MaintenanceState state, String languageKey) {
        Map<String, String> placeholders = scopePlaceholders(state, -1L);
        for (ProxyPlayer player : scopedPlayers(state)) {
            player.sendMessage(language.raw(languageKey, placeholders, player.protocolVersion()));
        }
    }

    private Map<String, String> scopePlaceholders(MaintenanceState state, long remainingMillis) {
        String scope = displayScope(state);
        boolean global = state.scopeType() == MaintenanceType.GLOBAL;
        // <where> empty for global so messages avoid "en global"
        String where = global ? "" : " en &f" + scope;
        if (remainingMillis >= 0) {
            return Map.of("scope", scope, "where", where, "time", DurationParser.format(remainingMillis));
        }
        return Map.of("scope", scope, "where", where);
    }

    private void kickAffected(MaintenanceState state) {
        for (ProxyPlayer player : platform.onlinePlayers()) {
            if (canBypass(player)) continue;

            if (!isPlayerInScope(state, player)) continue;

            if (state.scopeType() != MaintenanceType.GLOBAL && config.kickRedirectLobby()) {
                // Keep players on allow-list servers (e.g. lobby).
                if (isAllowedDuringMaintenance(player.currentServer())) continue;

                platform.findPlayerByUuid(player.uuid()).ifPresent(p -> {
                    // Best-effort lobby redirect via connect to first allow / hub name.
                    String lobby = config.allowDuringMaintenance().stream().findFirst().orElse("lobby");
                    platform.connectPlayer(p.uuid(), lobby);
                    p.sendMessage(language.raw("lobby.sending"));
                });
                continue;
            }

            platform.kickPlayer(player.uuid(), kickMessage(state, player.protocolVersion()));
        }
    }

    public boolean isAllowedDuringMaintenance(String server) {
        if (server == null || server.isBlank()) return false;

        for (String allowed : config.allowDuringMaintenance()) {
            if (allowed != null && allowed.equalsIgnoreCase(server)) return true;
        }

        return false;
    }

    private void tickExpirations() {
        long now = System.currentTimeMillis();

        for (MaintenanceState state : List.copyOf(states.values())) {
            if (state.active() && state.endAt() > 0 && now > state.endAt()) {
                disable(state.scopeType(), state.scopeId(), "TIMER");
            }
        }
    }

    private void broadcastScope(MaintenanceState state, String message) {
        MaintenanceType type = state.scopeType();
        if (type == MaintenanceType.GLOBAL) {
            platform.broadcast(message);
            return;
        }

        if (type == MaintenanceType.SERVER) {
            platform.broadcastToServer(state.scopeId(), message);
            return;
        }

        if (type == MaintenanceType.MODE) {
            for (String server : modes().getOrDefault(
                state.scopeId().toLowerCase(Locale.ROOT),
                config.serversForMode(state.scopeId())
            )) {
                platform.broadcastToServer(server, message);
            }
        }
    }

    private void actionBarScope(MaintenanceState state, String message) {
        for (ProxyPlayer player : scopedPlayers(state)) {
            platform.sendActionBar(player.uuid(), message);
        }
    }

    private void titleScope(MaintenanceState state, String title, String subtitle) {
        for (ProxyPlayer player : scopedPlayers(state)) {
            platform.sendTitle(player.uuid(), title, subtitle);
        }
    }

    private List<ProxyPlayer> scopedPlayers(MaintenanceState state) {
        List<ProxyPlayer> players = new ArrayList<>();
        for (ProxyPlayer player : platform.onlinePlayers()) {
            if (isPlayerInScope(state, player)) {
                players.add(player);
            }
        }

        return players;
    }

    private boolean isPlayerInScope(MaintenanceState state, ProxyPlayer player) {
        MaintenanceType type = state.scopeType();
        if (type == MaintenanceType.GLOBAL) return true;

        if (type == MaintenanceType.SERVER) {
            return state.scopeId().equalsIgnoreCase(player.currentServer());
        }

        if (type == MaintenanceType.MODE) {
            List<String> servers = modes().getOrDefault(
                state.scopeId().toLowerCase(Locale.ROOT),
                config.serversForMode(state.scopeId())
            );
            for (String server : servers) {
                if (server != null && server.equalsIgnoreCase(player.currentServer())) {
                    return true;
                }
            }
        }

        return false;
    }

    private void persistState(MaintenanceState state) {
        redis.withJedis(jedis -> {
            String redisKey = redisKey(state.key());
            jedis.hset(
                redisKey,
                Map.of(
                    "scope_type",
                    state.scopeType().name(),
                    "scope_id",
                    state.scopeId(),
                    "active",
                    Boolean.toString(state.active()),
                    "reason",
                    state.reason(),
                    "start_at",
                    Long.toString(state.startAt()),
                    "end_at",
                    Long.toString(state.endAt()),
                    "enabled_by",
                    state.enabledBy()
                )
            );
            return true;
        });
    }

    private void loadFromRedis() {
        redis.withJedis(jedis -> {
            Set<String> keys = jedis.keys(redis.keyPrefix() + ":maintenance:state:*");
            if (keys != null) {
                for (String key : keys) {
                    Map<String, String> map = jedis.hgetAll(key);
                    if (map == null || map.isEmpty()) continue;

                    MaintenanceState state = new MaintenanceState(
                        MaintenanceType.valueOf(map.getOrDefault("scope_type", "GLOBAL")),
                        map.getOrDefault("scope_id", "global"),
                        Boolean.parseBoolean(map.getOrDefault("active", "false")),
                        map.getOrDefault("reason", config.defaultReason()),
                        Long.parseLong(map.getOrDefault("start_at", "0")),
                        Long.parseLong(map.getOrDefault("end_at", "0")),
                        map.getOrDefault("enabled_by", "SYSTEM")
                    );
                    states.put(state.key(), state);
                }
            }

            String raw = jedis.get(redis.keyPrefix() + ":maintenance:whitelist");
            if (raw != null && !raw.isBlank()) {
                whitelist.clear();

                for (String part : raw.split(",")) {
                    if (!part.isBlank()) {
                        whitelist.add(part.toLowerCase(Locale.ROOT));
                    }
                }
            }

            return true;
        });
    }

    private void saveWhitelist() {
        config.yaml().saveTopLevelList("WHITELIST", new ArrayList<>(whitelist));
        redis.withJedis(jedis -> {
            jedis.set(redis.keyPrefix() + ":maintenance:whitelist", String.join(",", whitelist));
            return true;
        });
    }

    private void publishState(MaintenanceState state) {
        JsonObject data = new JsonObject();
        data.addProperty("scope_type", state.scopeType().name());
        data.addProperty("scope_id", state.scopeId());
        data.addProperty("active", state.active());
        data.addProperty("reason", state.reason());
        data.addProperty("start_at", state.startAt());
        data.addProperty("end_at", state.endAt());
        data.addProperty("enabled_by", state.enabledBy());
        redis.publish(new RedisEvent(ProxyEventType.MAINTENANCE_UPDATE.id(), proxyId, data));
    }

    private void publishWhitelist() {
        JsonObject data = new JsonObject();
        JsonArray array = new JsonArray();

        for (String entry : whitelist) {
            array.add(entry);
        }

        data.add("entries", array);
        redis.publish(new RedisEvent(ProxyEventType.MAINTENANCE_WHITELIST.id(), proxyId, data));
    }

    private void onRemoteState(RedisEvent event) {
        JsonObject data = event.data();
        MaintenanceState state = new MaintenanceState(
            MaintenanceType.valueOf(data.get("scope_type").getAsString()),
            data.get("scope_id").getAsString(),
            data.get("active").getAsBoolean(),
            data.get("reason").getAsString(),
            data.get("start_at").getAsLong(),
            data.get("end_at").getAsLong(),
            data.get("enabled_by").getAsString()
        );
        states.put(state.key(), state);
    }

    private void onRemoteWhitelist(RedisEvent event) {
        whitelist.clear();

        if (event.data().has("entries") && event.data().get("entries").isJsonArray()) {
            event
                .data()
                .getAsJsonArray("entries")
                .forEach(el -> whitelist.add(el.getAsString().toLowerCase(Locale.ROOT)));
        }
    }

    private String redisKey(String stateKey) {
        return redis.keyPrefix() + ":maintenance:state:" + stateKey;
    }

    private String text(String configured, String languageKey, Map<String, String> placeholders) {
        if (configured.isBlank()) return language.raw(languageKey, placeholders);

        return language.format(configured, placeholders);
    }

    private static String key(MaintenanceType scope, String id) {
        return scope.name().toLowerCase(Locale.ROOT) + ":" + id.toLowerCase(Locale.ROOT);
    }

    private static String displayScope(MaintenanceState state) {
        return state.scopeType() == MaintenanceType.GLOBAL ? "global" : state.scopeId();
    }

    private static List<String> normalizeAll(List<String> input) {
        Set<String> set = new LinkedHashSet<>();

        for (String value : input) {
            set.add(value.toLowerCase(Locale.ROOT));
        }

        return new ArrayList<>(set);
    }
}
