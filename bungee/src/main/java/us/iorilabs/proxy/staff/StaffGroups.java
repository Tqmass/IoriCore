package us.iorilabs.proxy.staff;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import us.iorilabs.proxy.config.PermissionsConfig;
import us.iorilabs.proxy.hook.luckperms.LuckPermsHook;
import us.iorilabs.proxy.player.ProxyPlayer;

public final class StaffGroups {

    private final Map<String, String> groups;
    private final String staffRootPermission;
    private final LuckPermsHook luckPerms;

    public StaffGroups(PermissionsConfig permissions, String staffRootPermission, LuckPermsHook luckPerms) {
        this.groups = new LinkedHashMap<>(permissions.staffGroups());
        this.staffRootPermission = staffRootPermission;
        this.luckPerms = luckPerms;
    }

    public Map<String, String> groups() {
        return Map.copyOf(groups);
    }

    public boolean isStaff(ProxyPlayer player) {
        if (player.hasPermission(staffRootPermission)) return true;

        for (String permission : groups.values()) {
            if (player.hasPermission(permission)) return true;
        }

        return false;
    }

    public String rankId(ProxyPlayer player) {
        for (Map.Entry<String, String> entry : groups.entrySet()) {
            if (player.hasPermission(entry.getValue())) return entry.getKey();
        }

        return "";
    }

    public String displayRank(ProxyPlayer player) {
        if (player == null) return "";

        return luckPerms.getPrefix(player.uuid()).trim();
    }

    public Optional<String> groupPermission(String rankId) {
        String permission = groups.get(rankId);
        if (permission == null || permission.isBlank()) return Optional.empty();

        return Optional.of(permission);
    }
}
