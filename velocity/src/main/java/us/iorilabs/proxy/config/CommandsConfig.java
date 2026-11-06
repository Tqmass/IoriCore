package us.iorilabs.proxy.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CommandsConfig {

    private final YamlConfig yaml;
    private final PermissionsConfig permissions;
    private final Map<String, String> primaryByAlias = new LinkedHashMap<>();
    private final Map<String, List<String>> aliasesByPrimary = new LinkedHashMap<>();
    private final Map<String, String> permissionOverride = new LinkedHashMap<>();

    public CommandsConfig(YamlConfig yaml, PermissionsConfig permissions) {
        this.yaml = yaml;
        this.permissions = permissions;
        reloadIndex();
    }

    public void reloadIndex() {
        primaryByAlias.clear();
        aliasesByPrimary.clear();
        permissionOverride.clear();
        permissions.reloadIndex();

        Map<String, Object> commands = yaml.getSection("commands");
        for (Map.Entry<String, Object> entry : commands.entrySet()) {
            String primary = entry.getKey().toLowerCase(Locale.ROOT);
            primaryByAlias.put(primary, primary);

            if (!(entry.getValue() instanceof Map<?, ?> raw)) {
                aliasesByPrimary.put(primary, List.of());
                continue;
            }

            Object aliasesObj = raw.get("ALIASES");
            if (aliasesObj instanceof List<?> list) {
                List<String> aliases = list
                    .stream()
                    .map(String::valueOf)
                    .map(s -> s.toLowerCase(Locale.ROOT))
                    .toList();
                aliasesByPrimary.put(primary, aliases);

                for (String alias : aliases) {
                    primaryByAlias.put(alias, primary);
                }
            } else {
                aliasesByPrimary.put(primary, List.of());
            }

            Object permission = raw.get("PERMISSION");
            if (permission != null && !String.valueOf(permission).isBlank()) {
                permissionOverride.put(normalize(primary), String.valueOf(permission));
            }
        }
    }

    public String rootName() {
        return yaml.getString("root.name", "ioriproxy").toLowerCase(Locale.ROOT);
    }

    public List<String> rootAliases() {
        return yaml
            .getStringList("root.aliases")
            .stream()
            .map(alias -> alias.toLowerCase(Locale.ROOT))
            .toList();
    }

    public String permission(String key) {
        String normalized = normalize(key);
        String override = permissionOverride.get(normalized);
        if (override != null) return override;

        return permissions.permission(normalized);
    }

    public String resolvePrimary(String label) {
        return primaryByAlias.get(label.toLowerCase(Locale.ROOT));
    }

    public List<String> aliases(String primary) {
        return aliasesByPrimary.getOrDefault(primary.toLowerCase(Locale.ROOT), Collections.emptyList());
    }

    public Map<String, List<String>> allCommands() {
        return Collections.unmodifiableMap(aliasesByPrimary);
    }

    private static String normalize(String key) {
        return key.replace('_', '-').toLowerCase(Locale.ROOT);
    }
}
