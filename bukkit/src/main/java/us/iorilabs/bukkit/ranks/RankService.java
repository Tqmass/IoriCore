package us.iorilabs.bukkit.ranks;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.java.JavaPlugin;
import us.iorilabs.api.hook.LuckPermsHook;
import us.iorilabs.api.redis.NetworkEventKeys;
import us.iorilabs.api.redis.NetworkEventType;
import us.iorilabs.api.storage.SqlStorage;
import us.iorilabs.bukkit.ranks.enums.LuckPermsMode;
import us.iorilabs.bukkit.ranks.enums.RankEditResult;
import us.iorilabs.bukkit.ranks.enums.RankField;
import us.iorilabs.bukkit.ranks.enums.RankFlag;
import us.iorilabs.bukkit.ranks.enums.RankScope;
import us.iorilabs.bukkit.ranks.enums.RankType;
import us.iorilabs.bukkit.ranks.grant.Grant;
import us.iorilabs.bukkit.ranks.grant.GrantService;
import us.iorilabs.bukkit.ranks.grant.GrantSql;
import us.iorilabs.bukkit.redis.CoreRedisService;

public final class RankService {

    private static final Pattern KEY_PATTERN = Pattern.compile("[A-Za-z0-9_\\-]{1,32}");
    private static final long EXPIRE_PERIOD_TICKS = 40L * 20L;
    private static final Set<String> DANGEROUS_PERMISSIONS = Set.of(
        "*",
        "*.*",
        "bukkit.command.op",
        "minecraft.command.op"
    );

    private final JavaPlugin plugin;
    private final Logger logger;
    private final SqlStorage storage;
    private final RankSql sql;
    private final RankYaml yaml;
    private final GrantService grants;
    private final boolean sqlMode;
    private final String serverName;
    private LuckPermsMode luckPermsMode;
    private final Map<String, Rank> byKey = new ConcurrentHashMap<>();
    private final Map<String, Rank> byName = new ConcurrentHashMap<>();
    private final Map<UUID, Rank> byId = new ConcurrentHashMap<>();
    private final Map<UUID, PermissionAttachment> attachments = new ConcurrentHashMap<>();
    private RankInheritance inheritance;
    private CoreRedisService redis;
    private LuckPermsHook luckPerms = new us.iorilabs.api.hook.NoopLuckPermsHook();
    private int expireTask = -1;

    public RankService(JavaPlugin plugin, SqlStorage storage, FileConfiguration settings) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.storage = storage;
        this.sql = new RankSql(storage, logger);
        this.yaml = new RankYaml(plugin, logger);
        String mode = settings == null ? "YAML" : settings.getString("RANKS.STORAGE", "YAML");
        this.sqlMode = mode != null && mode.trim().equalsIgnoreCase("SQL");
        this.serverName = settings == null ? "lobby-1" : settings.getString("SETTINGS.SERVER-NAME", "lobby-1");
        this.luckPermsMode = LuckPermsMode.from(
            settings == null ? "OFF" : settings.getString("RANKS.LUCKPERMS-MODE", "OFF")
        );
        this.grants = new GrantService(new GrantSql(storage, logger), serverName);
        this.inheritance = new RankInheritance(key -> byKey.get(normalize(key)), serverName);
    }

    public void redis(CoreRedisService redis) {
        this.redis = redis;
    }

    public void luckPerms(LuckPermsHook hook) {
        this.luckPerms = hook == null ? new us.iorilabs.api.hook.NoopLuckPermsHook() : hook;
    }

    public void ensureLuckPermsMode() {
        if (luckPermsMode != LuckPermsMode.SYNC) return;
        if (!luckPerms.available()) {
            logger.warning(
                "RANKS.LUCKPERMS-MODE=SYNC requires LuckPerms soft-hook · falling back to OFF (IoriCore attachments)"
            );
            luckPermsMode = LuckPermsMode.OFF;
            return;
        }
        logger.info("RANKS.LUCKPERMS-MODE=SYNC · pushing permission nodes to LP groups ic_<key>");
        List<Rank> snapshot = new ArrayList<>(byKey.values());
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            for (Rank rank : snapshot) pushSync(rank);
        });
    }

    public void start() {
        if (expireTask != -1) return;
        expireTask = Bukkit.getScheduler()
            .runTaskTimerAsynchronously(plugin, this::sweepExpired, EXPIRE_PERIOD_TICKS, EXPIRE_PERIOD_TICKS)
            .getTaskId();
    }

    public void stop() {
        if (expireTask != -1) {
            Bukkit.getScheduler().cancelTask(expireTask);
            expireTask = -1;
        }
        for (Player player : Bukkit.getOnlinePlayers()) clearAttachment(player);
        attachments.clear();
        grants.cacheClear();
    }

    public void load() {
        byKey.clear();
        byName.clear();
        byId.clear();
        inheritance.invalidate();

        List<Rank> loaded;
        if (sqlMode) {
            loaded = loadFromSql();
        } else {
            loaded = yaml.load();
        }
        for (Rank rank : loaded) register(rank);
        ensureDefault();
        inheritance = new RankInheritance(key -> byKey.get(normalize(key)), serverName);
        for (Rank rank : byKey.values()) dropCyclicParents(rank);
        logger.fine("Ranks loaded · " + byKey.size() + " · storage=" + (sqlMode ? "SQL" : "YAML"));
    }

    public void save(Rank rank) {
        if (rank == null || rank.id() == null) return;
        if (rank.key() == null || rank.key().isEmpty()) {
            rank.key(sanitizeKey(rank.name() == null ? "rank" : rank.name()));
        }
        if (rank.name() == null || rank.name().isEmpty()) rank.name(rank.key());
        dropCyclicParents(rank);
        if (rank.defaultRank()) clearOtherDefaults(rank);
        register(rank);
        inheritance.invalidate();

        if (sqlMode) {
            if (storage == null || !storage.isConnected()) return;
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                sql.upsert(rank, serverName);
                publishRank(rank.key(), "update");
                pushSync(rank);
            });
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                yaml.save(rank);
                publishRank(rank.key(), "update");
                pushSync(rank);
            });
        }
        refreshOnlineForRank(rank);
    }

    public RankEditResult create(String rawKey, int weight) {
        String key = sanitizeKey(rawKey);
        if (!KEY_PATTERN.matcher(key).matches()) return RankEditResult.INVALID_NAME;
        if (byKey.containsKey(key) || byName.containsKey(key)) return RankEditResult.ALREADY_EXISTS;

        Rank rank = new Rank();
        rank.id(Rank.idFor(key))
            .key(key)
            .name(key)
            .displayName(key)
            .weight(Math.max(0, weight))
            .type(RankType.NORMAL)
            .scope(RankScope.GLOBAL)
            .colorKey("GRAY")
            .prefix("")
            .suffix("")
            .parents(new ArrayList<>())
            .permissions(new ArrayList<>());
        save(rank);
        return RankEditResult.OK;
    }

    public RankEditResult delete(String rawKey) {
        Rank rank = resolve(rawKey).orElse(null);
        if (rank == null) return RankEditResult.UNKNOWN_RANK;
        if (rank.defaultRank()) return RankEditResult.DEFAULT_RANK;

        String key = rank.key();
        UUID id = rank.id();
        byKey.remove(key);
        byName.entrySet().removeIf(entry -> id.equals(entry.getValue().id()));
        byId.remove(id);
        for (Rank other : byKey.values()) {
            if (other.parents().removeIf(parent -> parent.equalsIgnoreCase(key))) saveQuiet(other);
        }
        inheritance.invalidate();
        grants.dropRank(id);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (sqlMode) sql.delete(id);
            else yaml.delete(key);
            publishRank(key, "delete");
        });
        for (Player player : Bukkit.getOnlinePlayers()) scheduleApply(player.getUniqueId());
        return RankEditResult.OK;
    }

    public RankEditResult setField(String rawKey, RankField field, String value) {
        Rank rank = resolve(rawKey).orElse(null);
        if (rank == null) return RankEditResult.UNKNOWN_RANK;
        if (value == null) return RankEditResult.INVALID_VALUE;

        switch (field) {
            case PREFIX:
                if (value.length() > 64) return RankEditResult.INVALID_VALUE;
                rank.prefix(value);
                break;
            case SUFFIX:
                if (value.length() > 64) return RankEditResult.INVALID_VALUE;
                rank.suffix(value);
                break;
            case COLOR:
                if (value.trim().isEmpty() || value.length() > 32) return RankEditResult.INVALID_VALUE;
                rank.colorKey(value.trim());
                break;
            case WEIGHT:
                try {
                    rank.weight(Integer.parseInt(value.trim()));
                } catch (NumberFormatException ex) {
                    return RankEditResult.INVALID_VALUE;
                }
                break;
            case DISPLAY:
                if (value.trim().isEmpty() || value.length() > 64) return RankEditResult.INVALID_VALUE;
                rank.displayName(value.trim());
                break;
            default:
                return RankEditResult.INVALID_VALUE;
        }
        save(rank);
        return RankEditResult.OK;
    }

    public RankEditResult setFlag(String rawKey, RankFlag flag, boolean value) {
        Rank rank = resolve(rawKey).orElse(null);
        if (rank == null || flag == null) return RankEditResult.UNKNOWN_RANK;
        rank.flag(flag, value);
        save(rank);
        return RankEditResult.OK;
    }

    public RankEditResult addPermission(String rawKey, String permission) {
        Rank rank = resolve(rawKey).orElse(null);
        if (rank == null) return RankEditResult.UNKNOWN_RANK;
        String node = normalizePerm(permission);
        if (node == null) return RankEditResult.INVALID_VALUE;
        if (
            rank
                .permissions()
                .stream()
                .anyMatch(entry -> entry.equalsIgnoreCase(node))
        ) {
            return RankEditResult.ALREADY_EXISTS;
        }
        rank.permissions().add(node);
        save(rank);
        return RankEditResult.OK;
    }

    public RankEditResult removePermission(String rawKey, String permission) {
        Rank rank = resolve(rawKey).orElse(null);
        if (rank == null) return RankEditResult.UNKNOWN_RANK;
        String node = normalizePerm(permission);
        if (node == null) return RankEditResult.INVALID_VALUE;
        boolean removed = rank.permissions().removeIf(entry -> entry.equalsIgnoreCase(node));
        if (!removed) return RankEditResult.NOT_PRESENT;
        save(rank);
        return RankEditResult.OK;
    }

    public RankEditResult addParent(String rawKey, String parentKey) {
        Rank rank = resolve(rawKey).orElse(null);
        Rank parent = resolve(parentKey).orElse(null);
        if (rank == null) return RankEditResult.UNKNOWN_RANK;
        if (parent == null) return RankEditResult.UNKNOWN_PARENT;
        if (rank.key().equals(parent.key())) return RankEditResult.CYCLE;
        if (inheritance.createsCycle(rank, parent.key())) return RankEditResult.CYCLE;
        if (
            rank
                .parents()
                .stream()
                .anyMatch(entry -> entry.equalsIgnoreCase(parent.key()))
        ) {
            return RankEditResult.ALREADY_EXISTS;
        }
        rank.parents().add(parent.key());
        save(rank);
        return RankEditResult.OK;
    }

    public RankEditResult removeParent(String rawKey, String parentKey) {
        Rank rank = resolve(rawKey).orElse(null);
        if (rank == null) return RankEditResult.UNKNOWN_RANK;
        String key = normalize(parentKey);
        boolean removed = rank.parents().removeIf(entry -> entry.equalsIgnoreCase(key));
        if (!removed) return RankEditResult.NOT_PRESENT;
        save(rank);
        return RankEditResult.OK;
    }

    public boolean wouldCreateCycle(Rank rank, String parentName) {
        return inheritance.createsCycle(rank, parentName);
    }

    public Optional<Rank> byName(String name) {
        return resolve(name);
    }

    public Optional<Rank> byId(UUID id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(byId.get(id));
    }

    public Optional<Rank> byKey(String key) {
        if (key == null) return Optional.empty();
        return Optional.ofNullable(byKey.get(normalize(key)));
    }

    public Optional<Rank> resolve(String token) {
        if (token == null || token.trim().isEmpty()) return Optional.empty();
        String key = normalize(token);
        Rank rank = byKey.get(key);
        if (rank != null) return Optional.of(rank);
        return Optional.ofNullable(byName.get(key));
    }

    public Collection<Rank> all() {
        List<Rank> ranks = new ArrayList<>(byKey.values());
        ranks.sort(Comparator.comparingInt((Rank rank) -> rank.weight()).reversed());
        return Collections.unmodifiableList(ranks);
    }

    public Map<String, Rank> registry() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(byKey));
    }

    public boolean sqlMode() {
        return sqlMode;
    }

    public LuckPermsMode luckPermsMode() {
        return luckPermsMode;
    }

    public GrantService grants() {
        return grants;
    }

    public Rank defaultRank() {
        for (Rank rank : byKey.values()) {
            if (rank.defaultRank()) return rank;
        }
        return null;
    }

    public Rank active(UUID playerId) {
        if (playerId == null) return defaultRank();

        Rank best = null;
        for (Grant grant : grants.active(playerId)) {
            Rank rank = byId.get(grant.rankId());
            if (rank == null || rank.has(RankFlag.HIDDEN)) continue;
            if (best == null || rank.weight() > best.weight()) best = rank;
        }
        return best == null ? defaultRank() : best;
    }

    public RankDisplay display(Player player) {
        if (player == null) return null;

        if (luckPermsMode == LuckPermsMode.READ) {
            RankDisplay fromLp = displayFromLuckPerms(player.getUniqueId());
            if (fromLp != null) return fromLp;
        }

        Rank rank = active(player.getUniqueId());
        if (rank == null) return null;
        return RankDisplay.of(rank);
    }

    public void loadPlayer(UUID playerId) {
        if (playerId == null) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (grants.available()) grants.load(playerId);
            Bukkit.getScheduler().runTask(plugin, () -> {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline()) applyAttachments(player);
            });
        });
    }

    public void unloadPlayer(UUID playerId) {
        if (playerId == null) return;
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) clearAttachment(player);
        attachments.remove(playerId);
        grants.unload(playerId);
    }

    public void applyAttachments(Player player) {
        if (player == null || !player.isOnline()) return;
        if (!luckPermsMode.appliesAttachments()) {
            clearAttachment(player);
            return;
        }

        clearAttachment(player);
        Map<String, Boolean> nodes = effectivePermissions(player.getUniqueId());
        if (nodes.isEmpty()) return;

        boolean admin = holdsAdminRank(player.getUniqueId());
        PermissionAttachment attachment = player.addAttachment(plugin);
        for (Map.Entry<String, Boolean> entry : nodes.entrySet()) {
            String node = entry.getKey();
            if (node == null || node.isBlank()) continue;
            if (!admin && isDangerousPermission(node)) continue;
            attachment.setPermission(node, entry.getValue());
        }
        attachments.put(player.getUniqueId(), attachment);
        player.recalculatePermissions();
    }

    private boolean holdsAdminRank(UUID playerId) {
        if (playerId == null) return false;
        for (Grant grant : grants.active(playerId)) {
            Rank rank = byId.get(grant.rankId());
            if (rank != null && rank.admin()) return true;
        }
        Rank active = active(playerId);
        return active != null && active.admin();
    }

    private static boolean isDangerousPermission(String node) {
        String normalized = node.trim().toLowerCase(Locale.ROOT);
        return DANGEROUS_PERMISSIONS.contains(normalized);
    }

    public Map<String, Boolean> effectivePermissions(UUID playerId) {
        Map<String, Boolean> out = new LinkedHashMap<>();
        List<Rank> ranks = new ArrayList<>();
        for (Grant grant : grants.active(playerId)) {
            Rank rank = byId.get(grant.rankId());
            if (rank != null) ranks.add(rank);
        }
        Rank active = active(playerId);
        if (active != null && ranks.stream().noneMatch(rank -> rank.id().equals(active.id()))) {
            ranks.add(active);
        }
        ranks.sort(Comparator.comparingInt((Rank rank) -> rank.weight()).reversed());
        for (Rank rank : ranks) {
            for (Map.Entry<String, Boolean> entry : inheritance.permissions(rank).entrySet()) {
                out.merge(entry.getKey(), entry.getValue(), (current, next) -> current && next);
            }
        }
        return out;
    }

    public Grant grant(UUID playerId, Rank rank, long durationMillis, String addedBy, String reason) {
        if (playerId == null || rank == null) return null;
        if (!grants.available()) return null;

        Grant created = grants.add(playerId, rank, durationMillis, addedBy, reason);
        if (created == null) return null;
        publishGrant(playerId, rank.key(), "add");
        pushUserInherit(playerId, rank, true);
        scheduleApply(playerId);
        return created;
    }

    public int ungrant(UUID playerId, Rank rank, String removedBy, String reason) {
        if (playerId == null || rank == null || !grants.available()) return 0;
        int removed = grants.remove(playerId, rank, removedBy, reason);
        if (removed > 0) {
            publishGrant(playerId, rank.key(), "remove");
            pushUserInherit(playerId, rank, false);
            scheduleApply(playerId);
        }
        return removed;
    }

    public void onRemoteRank(String key, String action) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            load();
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (Player player : Bukkit.getOnlinePlayers()) applyAttachments(player);
            });
        });
    }

    public void onRemoteGrant(UUID playerId) {
        if (playerId == null) return;
        Player online = Bukkit.getPlayer(playerId);
        if (online == null || !online.isOnline()) return;
        loadPlayer(playerId);
    }

    public void scheduleApply(UUID playerId) {
        if (playerId == null) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) applyAttachments(player);
        });
    }

    private void sweepExpired() {
        Set<UUID> changed = grants.sweepExpired();
        if (changed.isEmpty()) return;
        for (UUID playerId : changed) {
            publishGrant(playerId, "", "expire");
            scheduleApply(playerId);
        }
    }

    private List<Rank> loadFromSql() {
        if (storage == null || !storage.isConnected()) {
            logger.warning("RANKS.STORAGE=SQL but storage offline · falling back to ranks.yml");
            return yaml.load();
        }
        List<Rank> loaded = sql.loadAll(serverName);
        if (loaded == null || loaded.isEmpty()) {
            logger.warning("ic_ranks empty · seeding from ranks.yml");
            List<Rank> seed = yaml.load();
            for (Rank rank : seed) {
                Rank copy = rank;
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> sql.upsert(copy, serverName));
            }
            return seed;
        }
        return loaded;
    }

    private void ensureDefault() {
        Rank current = defaultRank();
        if (current != null) return;
        Rank member = byKey.get("default");
        if (member == null) member = byKey.get("member");
        if (member == null && !byKey.isEmpty()) {
            member = byKey
                .values()
                .stream()
                .min(Comparator.comparingInt((Rank rank) -> rank.weight()))
                .orElse(null);
        }
        if (member == null) {
            Rank rank = new Rank();
            rank.id(Rank.idFor("default"))
                .key("default")
                .name("default")
                .displayName("&7Default")
                .prefix("&7")
                .weight(0)
                .defaultRank(true)
                .scope(RankScope.GLOBAL)
                .colorKey("GRAY");
            register(rank);
            saveQuiet(rank);
            return;
        }
        member.defaultRank(true);
        saveQuiet(member);
    }

    private void clearOtherDefaults(Rank keep) {
        for (Rank rank : byKey.values()) {
            if (rank == keep) continue;
            if (!rank.defaultRank()) continue;
            rank.defaultRank(false);
            saveQuiet(rank);
        }
    }

    private void dropCyclicParents(Rank rank) {
        if (rank.parents() == null || rank.parents().isEmpty()) return;
        List<String> requested = new ArrayList<>(rank.parents());
        rank.parents(new ArrayList<>());
        for (String parent : requested) {
            if (parent == null || inheritance.createsCycle(rank, parent)) {
                logger.warning("Rank " + rank.key() + " · dropped cyclic parent " + parent);
                continue;
            }
            if (byKey.containsKey(normalize(parent))) rank.parents().add(normalize(parent));
        }
    }

    private void register(Rank rank) {
        if (rank == null || rank.id() == null) return;
        if (rank.key() == null || rank.key().isEmpty()) {
            rank.key(sanitizeKey(rank.name() == null ? "rank" : rank.name()));
        }
        if (rank.name() == null || rank.name().isEmpty()) rank.name(rank.key());
        byKey.entrySet().removeIf(entry -> rank.id().equals(entry.getValue().id()));
        byName.entrySet().removeIf(entry -> rank.id().equals(entry.getValue().id()));
        byKey.put(rank.key(), rank);
        byName.put(rank.name().toLowerCase(Locale.ROOT), rank);
        byId.put(rank.id(), rank);
    }

    private void saveQuiet(Rank rank) {
        register(rank);
        if (sqlMode) {
            if (storage != null && storage.isConnected()) {
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> sql.upsert(rank, serverName));
            }
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> yaml.save(rank));
        }
    }

    private void refreshOnlineForRank(Rank rank) {
        if (rank == null) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            Rank active = active(player.getUniqueId());
            if (active != null && (active.id().equals(rank.id()) || inheritance.lineage(active).contains(rank))) {
                applyAttachments(player);
            }
        }
    }

    private void clearAttachment(Player player) {
        if (player == null) return;
        PermissionAttachment attachment = attachments.remove(player.getUniqueId());
        if (attachment != null) {
            try {
                player.removeAttachment(attachment);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    private RankDisplay displayFromLuckPerms(UUID playerId) {
        if (luckPerms == null || !luckPerms.available()) return null;
        String prefix = luckPerms.prefix(playerId);
        String suffix = luckPerms.suffix(playerId);
        if ((prefix == null || prefix.isEmpty()) && (suffix == null || suffix.isEmpty())) return null;
        return new RankDisplay(
            "lp",
            "lp",
            prefix == null ? "" : prefix,
            prefix == null ? "" : prefix,
            suffix == null ? "" : suffix,
            "&7",
            0
        );
    }

    private void pushSync(Rank rank) {
        if (luckPermsMode != LuckPermsMode.SYNC || rank == null || !luckPerms.available()) return;
        String group = lpGroup(rank.key());
        if (group == null) return;
        luckPerms.syncGroup(group, rank.permissions());
    }

    private void pushUserInherit(UUID playerId, Rank rank, boolean add) {
        if (luckPermsMode != LuckPermsMode.SYNC || playerId == null || rank == null || !luckPerms.available()) return;
        String group = lpGroup(rank.key());
        if (group == null) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> luckPerms.setUserInherit(playerId, group, add));
    }

    private static String lpGroup(String key) {
        if (key == null || key.trim().isEmpty()) return null;
        return "ic_" + key.trim().toLowerCase(Locale.ROOT);
    }

    private void publishRank(String key, String action) {
        if (redis == null || !redis.enabled()) return;
        JsonObject data = new JsonObject();
        data.addProperty(NetworkEventKeys.RANK_KEY, key == null ? "" : key);
        data.addProperty(NetworkEventKeys.ACTION, action == null ? "update" : action);
        data.addProperty(NetworkEventKeys.SERVER, serverName);
        redis.publish(NetworkEventType.RANK_UPDATE, data);
    }

    private void publishGrant(UUID playerId, String rankKey, String action) {
        if (redis == null || !redis.enabled() || playerId == null) return;
        JsonObject data = new JsonObject();
        data.addProperty(NetworkEventKeys.UUID, playerId.toString());
        data.addProperty(NetworkEventKeys.RANK_KEY, rankKey == null ? "" : rankKey);
        data.addProperty(NetworkEventKeys.ACTION, action == null ? "update" : action);
        data.addProperty(NetworkEventKeys.SERVER, serverName);
        redis.publish(NetworkEventType.GRANT_UPDATE, data);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizePerm(String raw) {
        if (raw == null) return null;
        String value = raw.trim();
        if (value.isEmpty() || value.length() > 128) return null;
        return value;
    }

    private static String sanitizeKey(String name) {
        String trimmed = name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
        StringBuilder out = new StringBuilder(trimmed.length());
        for (int i = 0; i < trimmed.length(); i++) {
            char ch = trimmed.charAt(i);
            if ((ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9') || ch == '_' || ch == '-') out.append(ch);
            else out.append('_');
        }
        return out.length() == 0 ? "rank" : out.toString();
    }
}
