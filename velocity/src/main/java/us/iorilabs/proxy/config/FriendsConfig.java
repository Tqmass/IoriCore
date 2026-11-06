package us.iorilabs.proxy.config;

import java.util.Map;
import us.iorilabs.proxy.player.ProxyPlayer;

public final class FriendsConfig {

    private final YamlConfig yaml;
    private final SettingsConfig settings;

    public FriendsConfig(YamlConfig yaml, SettingsConfig settings) {
        this.yaml = yaml;
        this.settings = settings;
    }

    public int maxFriends(ProxyPlayer player) {
        int max = Math.max(1, yaml.getInt("friends.max_friends.default", 50));
        if (player == null) return max;

        for (Map.Entry<String, Object> entry : yaml.getSection("friends.max_friends.groups").entrySet()) {
            if (!(entry.getValue() instanceof Map<?, ?> raw)) continue;

            Object permission = raw.get("PERMISSION");
            Object limit = raw.get("MAX");
            if (permission == null || limit == null) continue;

            String node = String.valueOf(permission).trim();
            if (node.isBlank() || !player.hasPermission(node)) continue;

            int groupMax = parsePositive(limit, max);
            if (groupMax > max) max = groupMax;
        }

        return max;
    }

    public int maxFriends() {
        return Math.max(1, yaml.getInt("friends.max_friends.default", 50));
    }

    public int requestCooldownSeconds() {
        return settings.cooldownSeconds("friends-request", 10);
    }

    public boolean confirmRemove() {
        return yaml.getBoolean("friends.confirm_remove", true);
    }

    public boolean notifyJoin() {
        return settings.notification("friends", "join", true);
    }

    public boolean notifyQuit() {
        return settings.notification("friends", "quit", true);
    }

    public boolean notifyServerSwitch() {
        return settings.notification("friends", "server-switch", true);
    }

    private static int parsePositive(Object value, int fallback) {
        try {
            return Math.max(1, Integer.parseInt(String.valueOf(value).trim()));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
