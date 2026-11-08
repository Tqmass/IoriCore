package us.iorilabs.proxy.discord;

import com.google.gson.JsonObject;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import us.iorilabs.proxy.database.StorageService;
import us.iorilabs.proxy.database.mysql.MySqlStorageService;
import us.iorilabs.proxy.discord.enums.DiscordPunishmentType;
import us.iorilabs.proxy.logging.ProxyLogger;
import us.iorilabs.proxy.platform.ProxyPlatform;
import us.iorilabs.proxy.player.ProxyPlayer;
import us.iorilabs.proxy.punishment.BanCheckService;
import us.iorilabs.proxy.redis.ProxyEventType;
import us.iorilabs.proxy.redis.RedisEvent;
import us.iorilabs.proxy.redis.RedisService;
import us.iorilabs.proxy.utilities.DurationParser;

public final class DiscordPunishmentHandler {

    private final ProxyPlatform platform;
    private final StorageService storage;
    private final RedisService redis;
    private final BanCheckService banCheck;
    private final ProxyLogger logger;
    private final String proxyId;

    public DiscordPunishmentHandler(
        ProxyPlatform platform,
        StorageService storage,
        RedisService redis,
        BanCheckService banCheck,
        ProxyLogger logger,
        String proxyId
    ) {
        this.platform = platform;
        this.storage = storage;
        this.redis = redis;
        this.banCheck = banCheck;
        this.logger = logger;
        this.proxyId = proxyId;
    }

    public Result apply(
        DiscordPunishmentType type,
        String targetName,
        String durationRaw,
        String reason,
        String actor
    ) {
        if (type == null) return Result.fail("Unknown punishment type.");
        if (targetName == null || targetName.isBlank()) return Result.fail("Missing player name.");

        String player = targetName.trim();
        String sanitizedReason = sanitizeReason(reason);
        long durationMillis = 0L;

        if (type.requiresDuration()) {
            try {
                durationMillis = DurationParser.parseMillis(durationRaw);
            } catch (Exception ex) {
                return Result.fail("Invalid duration. Use e.g. `1h`, `30m`, `7d`.");
            }
            if (durationMillis <= 0L) return Result.fail("Duration must be positive.");
        }

        Optional<ResolvedPlayer> target = resolvePlayer(player);
        if (target.isEmpty()) return Result.fail("Player not found online or in database: `" + player + "`");

        ResolvedPlayer resolved = target.get();
        long now = System.currentTimeMillis();
        long expiresAt = type.requiresDuration() ? now + durationMillis : 0L;
        String durationLabel = type.requiresDuration()
            ? DurationParser.format(durationMillis)
            : type == DiscordPunishmentType.BAN
              ? "Permanent"
              : "—";

        if (type != DiscordPunishmentType.KICK) {
            if (redis == null || !redis.isConnected()) {
                return Result.fail("Could not store punishment (Redis unavailable for Core write).");
            }
        }

        if (type.disconnect() || type == DiscordPunishmentType.KICK) {
            platform.kickPlayer(
                resolved.uuid(),
                "&cRoyalCTW &8» &fYou were " + type.name().toLowerCase(Locale.ROOT) + "ed.\n&7" + sanitizedReason
            );
        }

        if (banCheck != null && (type == DiscordPunishmentType.BAN || type == DiscordPunishmentType.TEMPBAN)) {
            banCheck.invalidate(resolved.uuid());
        }

        publishRedis(type, resolved, actor, sanitizedReason, expiresAt, type != DiscordPunishmentType.KICK);
        logger.module("Discord", "punishment &f" + type.name() + " &7→ &f" + resolved.name() + " &7by &f" + actor);

        return Result.ok(
            resolved.name(),
            resolved.uuid(),
            type,
            sanitizedReason,
            durationLabel,
            actor == null || actor.isBlank() ? "Discord" : actor
        );
    }

    private void publishRedis(
        DiscordPunishmentType type,
        ResolvedPlayer target,
        String actor,
        String reason,
        long expiresAt,
        boolean persist
    ) {
        if (redis == null) return;

        JsonObject data = new JsonObject();
        data.addProperty("type", type.sqlType());
        data.addProperty("target_uuid", target.uuid().toString());
        data.addProperty("target_name", target.name());
        data.addProperty("actor", actor == null ? "Discord" : actor);
        data.addProperty("reason", reason);
        data.addProperty("expires_at", expiresAt);
        data.addProperty("persist", persist);
        redis.publish(new RedisEvent(ProxyEventType.PUNISHMENT_APPLIED.id(), proxyId, data));
    }


    private Optional<ResolvedPlayer> resolvePlayer(String name) {
        Optional<ProxyPlayer> online = platform.findPlayerByName(name);
        if (online.isPresent()) {
            return Optional.of(new ResolvedPlayer(online.get().uuid(), online.get().name()));
        }

        Optional<MySqlStorageService> mysql = storage == null ? Optional.empty() : storage.mysql();
        if (mysql.isEmpty() || !storage.isConnected()) return Optional.empty();

        try {
            return Optional.ofNullable(
                mysql.get().call(connection -> {
                    try (
                        PreparedStatement ps = connection.prepareStatement(
                            "SELECT uuid, name FROM ip_players WHERE LOWER(name) = LOWER(?) LIMIT 1"
                        )
                    ) {
                        ps.setString(1, name);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next()) return null;
                            return new ResolvedPlayer(UUID.fromString(rs.getString("uuid")), rs.getString("name"));
                        }
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                })
            );
        } catch (Exception ex) {
            logger.warn("Discord player lookup failed: " + ex.getMessage());
            return Optional.empty();
        }
    }

    private static String sanitizeReason(String reason) {
        if (reason == null || reason.isBlank()) return "No reason provided";
        return truncate(reason.trim(), 512);
    }

    private static String truncate(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, max);
    }

    public record ResolvedPlayer(UUID uuid, String name) {}

    public record Result(
        boolean success,
        String error,
        String player,
        UUID uuid,
        DiscordPunishmentType type,
        String reason,
        String duration,
        String actor
    ) {
        public static Result ok(
            String player,
            UUID uuid,
            DiscordPunishmentType type,
            String reason,
            String duration,
            String actor
        ) {
            return new Result(true, null, player, uuid, type, reason, duration, actor);
        }

        public static Result fail(String error) {
            return new Result(false, error, null, null, null, null, null, null);
        }

        public Map<String, String> placeholders() {
            return Map.of(
                "player",
                player == null ? "?" : player,
                "uuid",
                uuid == null ? "?" : uuid.toString(),
                "type",
                type == null ? "?" : type.name(),
                "reason",
                reason == null ? "?" : reason,
                "duration",
                duration == null ? "—" : duration,
                "actor",
                actor == null ? "Discord" : actor
            );
        }
    }
}
