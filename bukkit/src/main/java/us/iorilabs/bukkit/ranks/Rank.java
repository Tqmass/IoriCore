package us.iorilabs.bukkit.ranks;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import us.iorilabs.bukkit.ranks.enums.RankFlag;
import us.iorilabs.bukkit.ranks.enums.RankScope;
import us.iorilabs.bukkit.ranks.enums.RankType;

@Getter
@Setter
@Accessors(fluent = true)
public final class Rank {

    private UUID id;
    private String key;
    private String name;
    private String displayName;
    private String prefix = "";
    private String suffix = "";
    private int weight;
    private RankType type = RankType.NORMAL;
    private RankScope scope = RankScope.GLOBAL;
    private String colorKey;
    private boolean italic;
    private boolean bold;
    private boolean defaultRank;
    private Set<RankFlag> flags = EnumSet.noneOf(RankFlag.class);
    private List<String> parents = new ArrayList<>();
    private List<String> permissions = new ArrayList<>();

    public static UUID idFor(String key) {
        return UUID.nameUUIDFromBytes(("rank:" + key).getBytes(StandardCharsets.UTF_8));
    }

    public boolean has(RankFlag flag) {
        return flags.contains(flag);
    }

    public Rank flag(RankFlag flag, boolean value) {
        if (value) flags.add(flag);
        else flags.remove(flag);
        return this;
    }

    // ~ alias de weight y flags para los menús existentes (priority / booleanos sueltos)
    public int priority() {
        return weight;
    }

    public Rank priority(int value) {
        return weight(value);
    }

    public boolean staff() {
        return has(RankFlag.STAFF);
    }

    public Rank staff(boolean value) {
        return flag(RankFlag.STAFF, value);
    }

    public boolean admin() {
        return has(RankFlag.ADMIN);
    }

    public Rank admin(boolean value) {
        return flag(RankFlag.ADMIN, value);
    }

    public boolean media() {
        return has(RankFlag.MEDIA);
    }

    public Rank media(boolean value) {
        return flag(RankFlag.MEDIA, value);
    }

    public boolean visible() {
        return !has(RankFlag.HIDDEN);
    }

    public Rank visible(boolean value) {
        return flag(RankFlag.HIDDEN, !value);
    }

    @Deprecated
    public boolean op() {
        return false;
    }

    @Deprecated
    public Rank op(boolean ignored) {
        return this;
    }

    public int permissionCount() {
        return permissions.size();
    }

    public String display() {
        return displayName == null || displayName.isEmpty() ? name : displayName;
    }
}
