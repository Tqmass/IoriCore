package us.iorilabs.proxy.discord.link;

import com.google.gson.JsonObject;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.experimental.Accessors;
import us.iorilabs.api.redis.NetworkEventKeys;
import us.iorilabs.proxy.database.StorageService;
import us.iorilabs.proxy.discord.link.enums.SyncResult;
import us.iorilabs.proxy.language.LanguageManager;
import us.iorilabs.proxy.logging.ProxyLogger;
import us.iorilabs.proxy.platform.ProxyPlatform;
import us.iorilabs.proxy.player.ProxyPlayer;
import us.iorilabs.proxy.redis.ProxyEventType;
import us.iorilabs.proxy.redis.RedisEvent;
import us.iorilabs.proxy.redis.RedisService;

@Getter
@Accessors(fluent = true)
public final class DiscordLinkService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 10;
    private static final String NOT_LINKED = "";
    private static final String STATE_LINKED = "LINKED";
    private static final String STATE_UNLINKED = "UNLINKED";

    private final ProxyPlatform platform;
    private final RedisService redis;
    private final LanguageManager language;
    private final ProxyLogger logger;
    private final DiscordLinkSql sql;
    private final String proxyId;
    private volatile int codeTtlSeconds;
    private volatile int cooldownSeconds;
    private volatile int redeemCooldownSeconds;

    private final Map<UUID, String> linkCache = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastRequest = new ConcurrentHashMap<>();
    private final Map<String, Long> lastRedeemByDiscord = new ConcurrentHashMap<>();

    public DiscordLinkService(
        ProxyPlatform platform,
        RedisService redis,
        LanguageManager language,
        ProxyLogger logger,
        StorageService storage,
        String proxyId,
        int codeTtlSeconds,
        int cooldownSeconds
    ) {
        this(platform, redis, language, logger, storage, proxyId, codeTtlSeconds, cooldownSeconds, 5);
    }

    public DiscordLinkService(
        ProxyPlatform platform,
        RedisService redis,
        LanguageManager language,
        ProxyLogger logger,
        StorageService storage,
        String proxyId,
        int codeTtlSeconds,
        int cooldownSeconds,
        int redeemCooldownSeconds
    ) {
        this.platform = platform;
        this.redis = redis;
        this.language = language;
        this.logger = logger;
        this.sql = new DiscordLinkSql(storage, logger);
        this.proxyId = proxyId;
        this.codeTtlSeconds = codeTtlSeconds;
        this.cooldownSeconds = cooldownSeconds;
        this.redeemCooldownSeconds = Math.max(1, redeemCooldownSeconds);
        redis.subscribe(ProxyEventType.DISCORD_LINK_UPDATE.id(), this::onRemote);
    }

    public void reload(int codeTtlSeconds, int cooldownSeconds) {
        reload(codeTtlSeconds, cooldownSeconds, redeemCooldownSeconds);
    }

    public void reload(int codeTtlSeconds, int cooldownSeconds, int redeemCooldownSeconds) {
        this.codeTtlSeconds = codeTtlSeconds;
        this.cooldownSeconds = cooldownSeconds;
        this.redeemCooldownSeconds = Math.max(1, redeemCooldownSeconds);
    }

    public boolean available() {
        return sql.available();
    }

    public long cooldownRemaining(UUID uuid) {
        Long last = lastRequest.get(uuid);
        if (last == null) return 0L;
        long remaining = last + cooldownSeconds * 1000L - System.currentTimeMillis();
        return remaining <= 0L ? 0L : (remaining + 999L) / 1000L;
    }

    public void requestCode(ProxyPlayer player, Consumer<Optional<String>> callback) {
        lastRequest.put(player.uuid(), System.currentTimeMillis());
        platform.asyncExecutor().execute(() -> {
            String code = generateCode();
            long expiresAt = System.currentTimeMillis() + codeTtlSeconds * 1000L;
            boolean saved = sql.saveCode(code, player.uuid(), player.name(), expiresAt);
            callback.accept(saved ? Optional.of(code) : Optional.empty());
        });
    }

    public Redeem redeem(String rawCode, String discordId) {
        String code = rawCode == null ? "" : rawCode.trim().toUpperCase(Locale.ROOT);
        if (!isValidCode(code)) return new Redeem(SyncResult.INVALID_CODE, null, null);
        if (discordId == null || discordId.isBlank()) return new Redeem(SyncResult.UNAVAILABLE, null, null);
        if (isRedeemRateLimited(discordId)) return new Redeem(SyncResult.RATE_LIMITED, null, null);
        if (!sql.available()) return new Redeem(SyncResult.UNAVAILABLE, null, null);

        lastRedeemByDiscord.put(discordId, System.currentTimeMillis());

        Optional<DiscordLinkSql.PendingCode> pending = sql.consumeCode(code);
        if (pending.isEmpty()) return new Redeem(SyncResult.INVALID_CODE, null, null);

        DiscordLinkSql.PendingCode found = pending.get();
        Optional<UUID> previousOwner = sql.uuidByDiscord(discordId);
        if (!sql.link(found.uuid(), discordId, System.currentTimeMillis())) {
            return new Redeem(SyncResult.UNAVAILABLE, null, null);
        }

        previousOwner.filter(old -> !old.equals(found.uuid())).ifPresent(old -> applyUpdate(old, NOT_LINKED, true));
        applyUpdate(found.uuid(), discordId, true);
        publish(found.uuid(), discordId, found.name(), STATE_LINKED);
        logger.module("Discord", "linked &f" + found.name());
        return new Redeem(SyncResult.LINKED, found.uuid(), found.name());
    }

    public void unlink(UUID uuid, Consumer<Boolean> callback) {
        platform.asyncExecutor().execute(() -> {
            boolean removed = sql.unlink(uuid);
            if (removed) {
                applyUpdate(uuid, NOT_LINKED, false);
                publish(uuid, NOT_LINKED, "", STATE_UNLINKED);
            }
            callback.accept(removed);
        });
    }

    public Optional<String> discordId(UUID uuid) {
        if (uuid == null) return Optional.empty();

        String cached = linkCache.get(uuid);
        if (cached == null) {
            cached = sql.discordId(uuid).orElse(NOT_LINKED);
            linkCache.put(uuid, cached);
        }
        return cached.isEmpty() ? Optional.empty() : Optional.of(cached);
    }

    public Optional<UUID> uuidByDiscord(String discordId) {
        return sql.uuidByDiscord(discordId);
    }

    public void evict(UUID uuid) {
        if (uuid != null) linkCache.remove(uuid);
    }

    private boolean isRedeemRateLimited(String discordId) {
        Long last = lastRedeemByDiscord.get(discordId);
        if (last == null) return false;
        return System.currentTimeMillis() - last < redeemCooldownSeconds * 1000L;
    }

    private static boolean isValidCode(String code) {
        return code != null && code.matches("[A-Z0-9]{" + CODE_LENGTH + ",32}");
    }

    private static String generateCode() {
        char[] chars = new char[CODE_LENGTH];
        for (int i = 0; i < CODE_LENGTH; i++) {
            chars[i] = CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length()));
        }
        return new String(chars);
    }

    private void applyUpdate(UUID uuid, String discordId, boolean notify) {
        linkCache.put(uuid, discordId == null ? NOT_LINKED : discordId);
        if (!notify || discordId == null || discordId.isEmpty()) return;

        platform.findPlayerByUuid(uuid).ifPresent(player -> player.sendMessage(language.rawFor(player, "sync.linked")));
    }

    private void publish(UUID uuid, String discordId, String name, String state) {
        JsonObject data = new JsonObject();
        data.addProperty(NetworkEventKeys.UUID, uuid.toString());
        data.addProperty("discord_id", discordId);
        data.addProperty("name", name);
        data.addProperty(NetworkEventKeys.STATE, state);
        redis.publish(new RedisEvent(ProxyEventType.DISCORD_LINK_UPDATE.id(), proxyId, data));
    }

    private void onRemote(RedisEvent event) {
        if (proxyId.equals(event.originProxyId()) || event.data() == null) return;

        JsonObject data = event.data();
        try {
            UUID uuid = UUID.fromString(data.get(NetworkEventKeys.UUID).getAsString());
            boolean linked =
                data.has(NetworkEventKeys.STATE) && STATE_LINKED.equals(data.get(NetworkEventKeys.STATE).getAsString());
            String discordId = linked && data.has("discord_id") ? data.get("discord_id").getAsString() : NOT_LINKED;
            applyUpdate(uuid, discordId, linked);
        } catch (RuntimeException ignored) {}
    }

    public record Redeem(SyncResult result, UUID uuid, String name) {}
}
