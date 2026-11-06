package us.iorilabs.bukkit.discord.link;

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
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import us.iorilabs.api.modules.ModuleId;
import us.iorilabs.api.redis.NetworkEventKeys;
import us.iorilabs.api.redis.NetworkEventType;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.discord.link.enums.SyncResult;
import us.iorilabs.bukkit.language.CoreLanguage;
import us.iorilabs.bukkit.logging.BukkitLogger;
import us.iorilabs.bukkit.redis.CoreRedisService;

@Getter
@Accessors(fluent = true)
public final class DiscordLinkService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 10;
    private static final String NOT_LINKED = "";
    private static final String STATE_LINKED = "LINKED";
    private static final String STATE_UNLINKED = "UNLINKED";

    private final IoriCorePlugin plugin;
    private final CoreLanguage language;
    private final BukkitLogger logger;
    private final DiscordLinkSql sql;
    private volatile int codeTtlSeconds;
    private volatile int cooldownSeconds;
    private volatile int redeemCooldownSeconds;

    private final Map<UUID, String> linkCache = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastRequest = new ConcurrentHashMap<>();
    private final Map<String, Long> lastRedeemByDiscord = new ConcurrentHashMap<>();

    public DiscordLinkService(
        IoriCorePlugin plugin,
        DiscordLinkSql sql,
        int codeTtlSeconds,
        int cooldownSeconds,
        int redeemCooldownSeconds
    ) {
        this.plugin = plugin;
        this.language = plugin.language();
        this.logger = plugin.coreLogger();
        this.sql = sql;
        this.codeTtlSeconds = codeTtlSeconds;
        this.cooldownSeconds = cooldownSeconds;
        this.redeemCooldownSeconds = Math.max(1, redeemCooldownSeconds);

        CoreRedisService redis = plugin.redis();
        if (redis != null) {
            redis.subscribe(NetworkEventType.DISCORD_LINK_UPDATE, this::onRemote);
        }
    }

    public void reload(int codeTtlSeconds, int cooldownSeconds, int redeemCooldownSeconds) {
        this.codeTtlSeconds = Math.max(60, codeTtlSeconds);
        this.cooldownSeconds = Math.max(0, cooldownSeconds);
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

    public void requestCode(Player player, Consumer<Optional<String>> callback) {
        if (player == null) {
            callback.accept(Optional.empty());
            return;
        }

        lastRequest.put(player.getUniqueId(), System.currentTimeMillis());
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String code = generateCode();
            long expiresAt = System.currentTimeMillis() + codeTtlSeconds * 1000L;
            boolean saved = sql.saveCode(code, player.getUniqueId(), player.getName(), expiresAt);
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(saved ? Optional.of(code) : Optional.empty()));
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

        previousOwner.filter(old -> !old.equals(found.uuid())).ifPresent(old -> applyUpdate(old, NOT_LINKED, false));
        applyUpdate(found.uuid(), discordId, true);
        publish(found.uuid(), discordId, found.name(), STATE_LINKED);
        logger.module("Discord", "linked &f" + found.name());
        notifyStaffReg(found.uuid(), discordId, found.name());
        return new Redeem(SyncResult.LINKED, found.uuid(), found.name());
    }

    private void notifyStaffReg(UUID uuid, String discordId, String name) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (plugin.modules() == null) return;
            us.iorilabs.bukkit.modules.staffauth.StaffAuthEnforceModule auth = plugin
                .modules()
                .get(ModuleId.STAFF_AUTH);
            if (auth == null || auth.staffReg() == null) return;
            auth.staffReg().onDiscordLinked(uuid, discordId, name);
        });
    }

    public void unlink(UUID uuid, Consumer<Boolean> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean removed = sql.unlink(uuid);
            if (removed) {
                applyUpdate(uuid, NOT_LINKED, false);
                publish(uuid, NOT_LINKED, "", STATE_UNLINKED);
            }
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(removed));
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

        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.sendMessage(language.rawFor(player, "DISCORD.LINKED", Map.of("DISCORD", discordId)));
            }
        });
    }

    private void publish(UUID uuid, String discordId, String name, String state) {
        CoreRedisService redis = plugin.redis();
        if (redis == null || !redis.enabled()) return;

        JsonObject data = new JsonObject();
        data.addProperty(NetworkEventKeys.UUID, uuid.toString());
        data.addProperty(NetworkEventKeys.DISCORD_ID, discordId);
        data.addProperty(NetworkEventKeys.PLAYER, name == null ? "" : name);
        data.addProperty(NetworkEventKeys.STATE, state);
        redis.publish(NetworkEventType.DISCORD_LINK_UPDATE, data);
    }

    private void onRemote(us.iorilabs.bukkit.redis.CoreRedisEvent event) {
        if (event == null || event.data() == null) return;
        if (plugin.serverName().equals(event.origin())) return;

        JsonObject data = event.data();
        try {
            UUID uuid = UUID.fromString(data.get(NetworkEventKeys.UUID).getAsString());
            boolean linked =
                data.has(NetworkEventKeys.STATE) && STATE_LINKED.equals(data.get(NetworkEventKeys.STATE).getAsString());
            String discordId =
                linked && data.has(NetworkEventKeys.DISCORD_ID)
                    ? data.get(NetworkEventKeys.DISCORD_ID).getAsString()
                    : NOT_LINKED;
            applyUpdate(uuid, discordId, linked);
        } catch (RuntimeException ignored) {}
    }

    public record Redeem(SyncResult result, UUID uuid, String name) {}
}
