package us.iorilabs.bukkit.punishment;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import us.iorilabs.api.hook.LiteBansHook;
import us.iorilabs.api.hook.NoopLiteBansHook;
import us.iorilabs.api.punishment.PunishmentType;
import us.iorilabs.api.storage.SqlStorage;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.punishment.enums.PunishmentStatus;

@Getter
@Accessors(fluent = true)
public final class PunishmentService {

    private static final long MUTE_CACHE_TTL_MS = 30_000L;

    private final IoriCorePlugin plugin;
    private final Logger logger;
    private final PunishmentSql sql;
    private final PunishmentYamlStore yaml;
    private final boolean sqlMode;
    private final Map<UUID, Boolean> banCache = new ConcurrentHashMap<>();
    private final Map<UUID, MuteEntry> muteCache = new ConcurrentHashMap<>();
    private final Set<UUID> muteRefreshing = ConcurrentHashMap.newKeySet();
    private LiteBansHook liteBans = new NoopLiteBansHook();

    public PunishmentService(IoriCorePlugin plugin, SqlStorage storage, Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
        this.sql = new PunishmentSql(storage, logger);
        this.yaml = new PunishmentYamlStore(plugin.getDataFolder(), logger);
        this.sqlMode = storage != null && storage.isConnected();
        logger.fine("Punishments · persist=" + (sqlMode ? "MYSQL" : "YAML data/punishments.yml"));
    }

    public void start(LiteBansHook hook) {
        this.liteBans = hook == null ? new NoopLiteBansHook() : hook;
    }

    public void shutdown() {
        banCache.clear();
        muteCache.clear();
        liteBans = new NoopLiteBansHook();
    }

    public Optional<Punishment> get(long id) {
        return sqlMode ? sql.load(id) : yaml.load(id);
    }

    public boolean isBanned(UUID targetId) {
        if (targetId == null) return false;

        Boolean cached = banCache.get(targetId);
        if (cached != null) return cached;

        try {
            if (liteBans.available() && liteBans.isBanned(targetId)) {
                banCache.put(targetId, true);
                return true;
            }

            Optional<Punishment> active = loadActiveBan(targetId);
            boolean banned = active.isPresent() && !isExpired(active.get());
            banCache.put(targetId, banned);
            return banned;
        } catch (Exception ex) {
            logger.warning("Ban check failed (fail-open) for " + targetId + ": " + ex.getMessage());
            return false;
        }
    }

    public Optional<Punishment> activeBan(UUID targetId) {
        if (targetId == null) return Optional.empty();
        try {
            Optional<Punishment> active = loadActiveBan(targetId);
            if (active.isEmpty() || isExpired(active.get())) return Optional.empty();
            return active;
        } catch (Exception ex) {
            logger.warning("Active ban load failed for " + targetId + ": " + ex.getMessage());
            return Optional.empty();
        }
    }

    public boolean isMuted(UUID targetId) {
        return activeMute(targetId).isPresent();
    }

    public Optional<Punishment> activeMute(UUID targetId) {
        if (targetId == null) return Optional.empty();

        MuteEntry entry = muteCache.get(targetId);
        long now = System.currentTimeMillis();
        if (entry == null || now - entry.checkedAt() >= MUTE_CACHE_TTL_MS) {
            if (Bukkit.isPrimaryThread()) {
                refreshMuteAsync(targetId);
            } else {
                entry = refreshMute(targetId);
            }
        }
        if (entry == null || entry.mute() == null) return Optional.empty();
        if (isExpired(entry.mute())) {
            muteCache.put(targetId, new MuteEntry(null, now));
            return Optional.empty();
        }
        return Optional.of(entry.mute());
    }

    public boolean blockIfMuted(Player player) {
        if (player == null) return false;
        Optional<Punishment> mute = activeMute(player.getUniqueId());
        if (mute.isEmpty()) return false;
        String reason = sanitizeReason(mute.get().reason());
        player.sendMessage(plugin.language().rawFor(player, "PUNISHMENTS.MUTE.BLOCKED", Map.of("reason", reason)));
        return true;
    }

    public MuteEntry refreshMute(UUID targetId) {
        MuteEntry entry = new MuteEntry(loadMute(targetId), System.currentTimeMillis());
        muteCache.put(targetId, entry);
        return entry;
    }

    public Punishment punish(
        PunishmentType type,
        UUID targetId,
        String targetName,
        UUID actorId,
        String actorName,
        String reason,
        long expiresAt,
        boolean silent
    ) {
        return punish(type, targetId, targetName, actorId, actorName, reason, expiresAt, silent, true);
    }

    public Punishment punishFromNetwork(
        PunishmentType type,
        UUID targetId,
        String targetName,
        String actorName,
        String reason,
        long expiresAt
    ) {
        return punish(type, targetId, targetName, null, actorName, reason, expiresAt, false, false);
    }

    private Punishment punish(
        PunishmentType type,
        UUID targetId,
        String targetName,
        UUID actorId,
        String actorName,
        String reason,
        long expiresAt,
        boolean silent,
        boolean syncNetwork
    ) {
        PunishmentType resolved = type == null ? PunishmentType.WARN : type;
        Punishment punishment = new Punishment();
        punishment
            .type(resolved)
            .status(PunishmentStatus.ACTIVE)
            .targetId(targetId)
            .targetName(targetName)
            .actorId(actorId)
            .actorName(actorName == null || actorName.trim().isEmpty() ? "Console" : actorName.trim())
            .reason(sanitizeReason(reason))
            .createdAt(System.currentTimeMillis())
            .expiresAt(Math.max(0L, expiresAt))
            .silent(silent);

        if (resolved == PunishmentType.BAN || resolved == PunishmentType.TEMPBAN) {
            if (targetId != null) banCache.put(targetId, true);
        }
        if (resolved == PunishmentType.MUTE || resolved == PunishmentType.TEMPMUTE) {
            if (targetId != null) muteCache.put(targetId, new MuteEntry(punishment, System.currentTimeMillis()));
        }

        persistNow(punishment);
        if (syncNetwork && plugin.punishmentSync() != null) plugin.punishmentSync().applied(punishment);
        return punishment;
    }

    public Optional<Punishment> activeWarn(UUID targetId) {
        if (targetId == null) return Optional.empty();
        try {
            Optional<Punishment> active = sqlMode ? sql.loadActiveWarn(targetId) : yaml.loadActiveWarn(targetId);
            if (active.isEmpty() || isExpired(active.get())) return Optional.empty();
            return active;
        } catch (Exception ex) {
            logger.warning("Active warn load failed for " + targetId + ": " + ex.getMessage());
            return Optional.empty();
        }
    }

    public Optional<Punishment> findForRevoke(String input, PunishmentType primary, PunishmentType secondary) {
        if (input == null || input.trim().isEmpty()) return Optional.empty();

        String token = input.trim();
        if (isNumericId(token)) {
            long id = Long.parseLong(token);
            Optional<Punishment> loaded = get(id);
            if (loaded.isEmpty()) return Optional.empty();
            Punishment punishment = loaded.get();
            if (!matchesRevokeType(punishment, primary, secondary)) return Optional.empty();
            if (!isRevokable(punishment)) return Optional.empty();
            return loaded;
        }

        UUID asUuid = parseUuid(token);
        UUID targetId;
        if (asUuid != null) {
            targetId = asUuid;
        } else {
            OfflinePlayer target = PunishmentActions.resolveTarget(token);
            if (target == null || target.getUniqueId() == null) return Optional.empty();
            targetId = target.getUniqueId();
        }
        return loadActiveForRevoke(targetId, primary, secondary);
    }

    public boolean markRevoked(Punishment punishment) {
        if (punishment == null || !isRevokable(punishment)) return false;

        punishment.status(PunishmentStatus.REVOKED);
        persistNow(punishment);
        revoked(punishment.targetId(), punishment.type());
        return true;
    }

    public void revoked(UUID targetId, PunishmentType type) {
        if (targetId == null) return;
        invalidateBanCache(targetId);
        invalidateMuteCache(targetId);
        if (plugin.punishmentSync() != null) plugin.punishmentSync().invalidated(targetId, type);
    }

    public void invalidateBanCache(UUID targetId) {
        if (targetId == null) return;
        banCache.remove(targetId);
    }

    public void invalidateMuteCache(UUID targetId) {
        if (targetId == null) return;
        muteCache.remove(targetId);
    }

    public static long parseDurationMillis(String raw) {
        if (raw == null || raw.trim().isEmpty()) return -1L;

        String token = raw.trim().toLowerCase(Locale.ROOT);
        if (token.equals("perm") || token.equals("permanent") || token.equals("-1")) return 0L;

        char unit = token.charAt(token.length() - 1);
        String numberPart = token.substring(0, token.length() - 1);
        if (numberPart.isEmpty()) return -1L;

        long amount;
        try {
            amount = Long.parseLong(numberPart);
        } catch (NumberFormatException ex) {
            return -1L;
        }
        if (amount <= 0L) return -1L;

        long multiplier;
        switch (unit) {
            case 's':
                multiplier = 1000L;
                break;
            case 'm':
                multiplier = 60_000L;
                break;
            case 'h':
                multiplier = 3_600_000L;
                break;
            case 'd':
                multiplier = 86_400_000L;
                break;
            case 'w':
                multiplier = 604_800_000L;
                break;
            default:
                return -1L;
        }
        return amount * multiplier;
    }

    public static boolean looksLikeDuration(String raw) {
        return parseDurationMillis(raw) >= 0L;
    }

    public String sanitizeReason(String reason) {
        if (reason == null || reason.trim().isEmpty()) return plugin.language().plain("PUNISHMENTS.NO-REASON");
        String trimmed = reason.trim();
        if (trimmed.length() > 512) return trimmed.substring(0, 512);
        return trimmed;
    }

    private Optional<Punishment> loadActiveBan(UUID targetId) {
        return sqlMode ? sql.loadActiveBan(targetId) : yaml.loadActiveBan(targetId);
    }

    private void persistNow(Punishment punishment) {
        try {
            if (sqlMode) sql.upsert(punishment);
            else yaml.upsert(punishment);
        } catch (Exception ex) {
            logger.warning("Punishment persist failed: " + ex.getMessage());
        }
    }

    private void refreshMuteAsync(UUID targetId) {
        if (!muteRefreshing.add(targetId)) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                refreshMute(targetId);
            } finally {
                muteRefreshing.remove(targetId);
            }
        });
    }

    private Punishment loadMute(UUID targetId) {
        try {
            Optional<Punishment> active = sqlMode ? sql.loadActiveMute(targetId) : yaml.loadActiveMute(targetId);
            if (active.isPresent() && !isExpired(active.get())) return active.get();
            if (liteBans.available() && liteBans.isMuted(targetId)) {
                return new Punishment().type(PunishmentType.MUTE).status(PunishmentStatus.ACTIVE).targetId(targetId);
            }
            return null;
        } catch (Exception ex) {
            logger.warning("Mute check failed (fail-open) for " + targetId + ": " + ex.getMessage());
            return null;
        }
    }

    private static boolean isExpired(Punishment punishment) {
        if (punishment == null) return true;
        if (punishment.status() != PunishmentStatus.ACTIVE) return true;
        long expires = punishment.expiresAt();
        return expires > 0L && expires <= System.currentTimeMillis();
    }

    private static boolean isRevokable(Punishment punishment) {
        return punishment != null && punishment.status() == PunishmentStatus.ACTIVE && !isExpired(punishment);
    }

    private static boolean isNumericId(String raw) {
        if (raw == null || raw.isEmpty()) return false;
        for (int i = 0; i < raw.length(); i++) {
            if (!Character.isDigit(raw.charAt(i))) return false;
        }
        return true;
    }

    private static UUID parseUuid(String raw) {
        if (raw == null || raw.length() < 32) return null;
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static boolean matchesRevokeType(Punishment punishment, PunishmentType primary, PunishmentType secondary) {
        if (punishment == null || punishment.type() == null) return false;
        PunishmentType type = punishment.type();
        return type == primary || type == secondary;
    }

    private Optional<Punishment> loadActiveForRevoke(UUID targetId, PunishmentType primary, PunishmentType secondary) {
        Optional<Punishment> active;
        if (primary == PunishmentType.BAN) {
            active = loadActiveBan(targetId);
        } else if (primary == PunishmentType.MUTE) {
            active = sqlMode ? sql.loadActiveMute(targetId) : yaml.loadActiveMute(targetId);
        } else {
            active = sqlMode ? sql.loadActiveWarn(targetId) : yaml.loadActiveWarn(targetId);
        }
        if (active.isEmpty() || !isRevokable(active.get())) return Optional.empty();
        Punishment punishment = active.get();
        if (!matchesRevokeType(punishment, primary, secondary)) return Optional.empty();
        return active;
    }

    public record MuteEntry(Punishment mute, long checkedAt) {}
}
