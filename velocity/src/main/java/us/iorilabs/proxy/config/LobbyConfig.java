package us.iorilabs.proxy.config;

import java.util.List;
import java.util.Locale;

public final class LobbyConfig {

    private final YamlConfig yaml;

    public LobbyConfig(YamlConfig yaml) {
        this.yaml = yaml;
    }

    public int pingDelayMs() {
        return Math.max(0, yaml.getInt("ping_delay", 1000));
    }

    public int pingTimeoutMs() {
        return Math.max(100, yaml.getInt("ping_timeout", 3000));
    }

    public List<String> fallbackServers() {
        List<String> grouped = yaml.getStringList("fallback.groups.default");
        if (!grouped.isEmpty()) return grouped;

        return yaml.getStringList("fallback_servers");
    }

    public String fallbackMode() {
        return yaml.getString("fallback.mode", "DEFAULT").toUpperCase(Locale.ROOT);
    }

    public List<String> lobbies() {
        List<String> list = yaml.getStringList("multilobby.lobbies");
        return list.isEmpty() ? fallbackServers() : list;
    }

    public boolean multilobbyEnabled() {
        return yaml.getBoolean("multilobby.enabled", true);
    }

    public String balancing() {
        return yaml.getString("multilobby.balancing", "least_players").toLowerCase(Locale.ROOT);
    }

    public boolean hubEnabled() {
        return yaml.getBoolean("hub.enabled", true);
    }

    public boolean onServerKick() {
        return yaml.getBoolean("on_server_kick", true);
    }

    public boolean onServerDown() {
        return yaml.getBoolean("on_server_down", true);
    }

    public List<String> ignoredReasons() {
        return yaml
            .getStringList("ignored_reasons")
            .stream()
            .map(reason -> reason.toLowerCase(Locale.ROOT))
            .toList();
    }

    public boolean clearChat() {
        return yaml.getBoolean("clear_chat", true);
    }

    public boolean joinBalancingEnabled() {
        return yaml.getBoolean("join_balancing.enabled", true);
    }

    public String joinBalancingGroup() {
        return yaml.getString("join_balancing.group", "DEFAULT");
    }

    public boolean isIgnoredKickReason(String plainReason) {
        if (plainReason == null || plainReason.isBlank()) return false;

        String lower = plainReason.toLowerCase(Locale.ROOT);
        for (String ignored : ignoredReasons()) {
            if (!ignored.isBlank() && lower.contains(ignored)) return true;
        }

        return false;
    }
}
