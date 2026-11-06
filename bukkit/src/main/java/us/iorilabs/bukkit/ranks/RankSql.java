package us.iorilabs.bukkit.ranks;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.RequiredArgsConstructor;
import us.iorilabs.api.storage.SqlStorage;
import us.iorilabs.bukkit.ranks.enums.RankFlag;
import us.iorilabs.bukkit.ranks.enums.RankScope;
import us.iorilabs.bukkit.ranks.enums.RankType;

@RequiredArgsConstructor
public final class RankSql {

    private static final String SELECT =
        "SELECT id, rank_key, name, display_name, prefix, suffix, priority, type, color_key, italic, bold, " +
        "staff, media, admin, visible, flags, default_rank, scope, server_key, parents FROM ic_ranks " +
        "WHERE scope = ? OR (scope = ? AND server_key = ?)";

    private static final String UPSERT =
        "INSERT INTO ic_ranks (id, rank_key, name, display_name, prefix, suffix, priority, type, color_key, " +
        "italic, bold, staff, media, admin, operator, visible, flags, default_rank, scope, server_key, parents) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?, ?, ?, ?, ?, ?) " +
        "ON DUPLICATE KEY UPDATE rank_key = VALUES(rank_key), name = VALUES(name), " +
        "display_name = VALUES(display_name), prefix = VALUES(prefix), suffix = VALUES(suffix), " +
        "priority = VALUES(priority), type = VALUES(type), color_key = VALUES(color_key), " +
        "italic = VALUES(italic), bold = VALUES(bold), staff = VALUES(staff), media = VALUES(media), " +
        "admin = VALUES(admin), operator = 0, visible = VALUES(visible), flags = VALUES(flags), " +
        "default_rank = VALUES(default_rank), scope = VALUES(scope), server_key = VALUES(server_key), " +
        "parents = VALUES(parents)";

    private final SqlStorage storage;
    private final Logger logger;

    public boolean available() {
        return storage != null && storage.isConnected();
    }

    public List<Rank> loadAll(String serverName) {
        if (!available()) return Collections.emptyList();

        Map<UUID, Rank> ranks = new LinkedHashMap<>();
        try (Connection connection = storage.connection(); PreparedStatement ps = connection.prepareStatement(SELECT)) {
            ps.setString(1, RankScope.GLOBAL.name());
            ps.setString(2, RankScope.SERVER.name());
            ps.setString(3, serverName == null ? "" : serverName.trim());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Rank rank = map(rs);
                    ranks.put(rank.id(), rank);
                }
            }
            if (!ranks.isEmpty()) loadPermissions(connection, ranks);
        } catch (SQLException ex) {
            logger.log(Level.WARNING, "Rank load failed", ex);
            return Collections.emptyList();
        }
        return new ArrayList<>(ranks.values());
    }

    public void upsert(Rank rank, String serverName) {
        if (!available() || rank == null || rank.id() == null) return;

        String serverKey = rank.scope() == RankScope.GLOBAL ? "" : serverName == null ? "" : serverName.trim();
        try (Connection connection = storage.connection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement ps = connection.prepareStatement(UPSERT)) {
                    bind(ps, rank, serverKey);
                    ps.executeUpdate();
                }
                replacePermissions(connection, rank);
                connection.commit();
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            logger.log(Level.WARNING, "Rank upsert failed id=" + rank.id(), ex);
        }
    }

    public void delete(UUID id) {
        if (!available() || id == null) return;

        try (Connection connection = storage.connection()) {
            try (
                PreparedStatement ps = connection.prepareStatement("DELETE FROM ic_rank_permissions WHERE rank_id = ?")
            ) {
                ps.setString(1, id.toString());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM ic_ranks WHERE id = ?")) {
                ps.setString(1, id.toString());
                ps.executeUpdate();
            }
        } catch (SQLException ex) {
            logger.log(Level.WARNING, "Rank delete failed id=" + id, ex);
        }
    }

    private void replacePermissions(Connection connection, Rank rank) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM ic_rank_permissions WHERE rank_id = ?")) {
            ps.setString(1, rank.id().toString());
            ps.executeUpdate();
        }
        if (rank.permissions().isEmpty()) return;

        try (
            PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO ic_rank_permissions (rank_id, permission) VALUES (?, ?)"
            )
        ) {
            for (String permission : new LinkedHashSet<>(rank.permissions())) {
                ps.setString(1, rank.id().toString());
                ps.setString(2, permission);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void loadPermissions(Connection connection, Map<UUID, Rank> ranks) throws SQLException {
        try (
            PreparedStatement ps = connection.prepareStatement("SELECT rank_id, permission FROM ic_rank_permissions");
            ResultSet rs = ps.executeQuery()
        ) {
            while (rs.next()) {
                Rank rank = ranks.get(parseUuid(rs.getString("rank_id")));
                if (rank != null) rank.permissions().add(rs.getString("permission"));
            }
        }
    }

    private static void bind(PreparedStatement ps, Rank rank, String serverKey) throws SQLException {
        ps.setString(1, rank.id().toString());
        ps.setString(2, rank.key());
        ps.setString(3, rank.name() == null ? rank.key() : rank.name());
        ps.setString(4, rank.displayName());
        ps.setString(5, rank.prefix());
        ps.setString(6, rank.suffix());
        ps.setInt(7, rank.weight());
        ps.setString(8, rank.type().name());
        ps.setString(9, rank.colorKey());
        ps.setInt(10, rank.italic() ? 1 : 0);
        ps.setInt(11, rank.bold() ? 1 : 0);
        ps.setInt(12, rank.staff() ? 1 : 0);
        ps.setInt(13, rank.media() ? 1 : 0);
        ps.setInt(14, rank.admin() ? 1 : 0);
        ps.setInt(15, rank.visible() ? 1 : 0);
        ps.setString(16, joinFlags(rank.flags()));
        ps.setInt(17, rank.defaultRank() ? 1 : 0);
        ps.setString(18, rank.scope().name());
        ps.setString(19, serverKey);
        ps.setString(20, String.join(",", rank.parents()));
    }

    private static Rank map(ResultSet rs) throws SQLException {
        String name = rs.getString("name");
        String key = rs.getString("rank_key");
        if (key == null || key.trim().isEmpty()) key = name;
        key = key.toLowerCase(Locale.ROOT);

        UUID id = parseUuid(rs.getString("id"));
        Set<RankFlag> flags = splitFlags(rs.getString("flags"));
        if (rs.getInt("staff") == 1) flags.add(RankFlag.STAFF);
        if (rs.getInt("media") == 1) flags.add(RankFlag.MEDIA);
        if (rs.getInt("admin") == 1) flags.add(RankFlag.ADMIN);
        if (rs.getInt("visible") == 0) flags.add(RankFlag.HIDDEN);

        Rank rank = new Rank();
        rank.id(id == null ? Rank.idFor(key) : id)
            .key(key)
            .name(name)
            .displayName(rs.getString("display_name"))
            .prefix(nullToEmpty(rs.getString("prefix")))
            .suffix(nullToEmpty(rs.getString("suffix")))
            .weight(rs.getInt("priority"))
            .type(RankType.from(rs.getString("type")))
            .colorKey(rs.getString("color_key"))
            .italic(rs.getInt("italic") == 1)
            .bold(rs.getInt("bold") == 1)
            .defaultRank(rs.getInt("default_rank") == 1)
            .scope(RankScope.from(rs.getString("scope")))
            .flags(flags)
            .parents(splitParents(rs.getString("parents")));
        return rank;
    }

    private static Set<RankFlag> splitFlags(String raw) {
        Set<RankFlag> flags = EnumSet.noneOf(RankFlag.class);
        if (raw == null || raw.isEmpty()) return flags;
        for (String part : raw.split(",")) {
            RankFlag flag = RankFlag.from(part);
            if (flag != null) flags.add(flag);
        }
        return flags;
    }

    private static String joinFlags(Set<RankFlag> flags) {
        List<String> names = new ArrayList<>();
        for (RankFlag flag : flags) names.add(flag.name());
        return String.join(",", names);
    }

    private static List<String> splitParents(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) return out;
        for (String part : raw.split(",")) {
            String trimmed = part.trim().toLowerCase(Locale.ROOT);
            if (!trimmed.isEmpty()) out.add(trimmed);
        }
        return out;
    }

    private static UUID parseUuid(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
