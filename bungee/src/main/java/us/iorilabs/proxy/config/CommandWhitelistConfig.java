package us.iorilabs.proxy.config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import us.iorilabs.proxy.commands.CommandBlockerMode;

public final class CommandWhitelistConfig {

    public static final String WILDCARD = "*";

    private final YamlConfig yaml;
    private final PermissionsConfig permissions;

    public CommandWhitelistConfig(YamlConfig yaml, PermissionsConfig permissions) {
        this.yaml = yaml;
        this.permissions = permissions;
    }

    public record Group(String name, String permission, Set<String> commands) {
        public boolean everyone() {
            return permission.isBlank();
        }

        public boolean allows(String root) {
            return commands.contains(WILDCARD) || commands.contains(root);
        }
    }

    public enum Scope {
        PROXY,
        ALL,
    }

    public CommandBlockerMode mode() {
        return CommandBlockerMode.from(yaml.getString("mode", "whitelist"));
    }

    public Scope scope() {
        return "all".equalsIgnoreCase(yaml.getString("scope", "proxy").trim()) ? Scope.ALL : Scope.PROXY;
    }

    public String bypassPermission() {
        return permissions.permission("commandwhitelist-bypass");
    }

    public boolean hideWithoutPermission() {
        return yaml.getBoolean("hide_without_permission", true);
    }

    public Set<String> globalBlocked() {
        return normalizeAll(yaml.getStringList("global_blocked"));
    }

    public List<Group> groups() {
        List<Group> groups = new ArrayList<>();

        for (Map.Entry<String, Object> entry : yaml.getSection("groups").entrySet()) {
            if (!(entry.getValue() instanceof Map<?, ?> raw)) continue;

            Object permission = raw.get("PERMISSION");
            List<String> commands = commandList(raw);
            groups.add(
                new Group(
                    entry.getKey().toLowerCase(Locale.ROOT),
                    permission == null ? "" : String.valueOf(permission).trim(),
                    normalizeAll(commands)
                )
            );
        }

        return groups;
    }

    private static List<String> commandList(Map<?, ?> raw) {
        Object commands = raw.get("COMMANDS");
        if (commands instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }

        Object legacy = raw.get("ALLOW");
        if (legacy instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }

        return List.of();
    }

    private static Set<String> normalizeAll(List<String> values) {
        Set<String> out = new HashSet<>();

        for (String value : values) {
            if (value == null || value.isBlank()) continue;

            String root = value.trim().toLowerCase(Locale.ROOT);
            out.add(root.startsWith("/") ? root.substring(1) : root);
        }

        return out;
    }
}
