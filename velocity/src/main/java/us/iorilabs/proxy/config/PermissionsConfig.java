package us.iorilabs.proxy.config;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class PermissionsConfig {

    private static final String[] SECTIONS = { "permissions", "extra-permissions" };

    private final YamlConfig yaml;
    private final Map<String, String> permissionByKey = new LinkedHashMap<>();

    public PermissionsConfig(YamlConfig yaml) {
        this.yaml = yaml;
        reloadIndex();
    }

    public void reloadIndex() {
        permissionByKey.clear();

        for (String section : SECTIONS) {
            for (Map.Entry<String, Object> entry : yaml.getSection(section).entrySet()) {
                permissionByKey.put(normalize(entry.getKey()), String.valueOf(entry.getValue()));
            }
        }
    }

    public String permission(String key) {
        return permissionByKey.getOrDefault(normalize(key), "ioriproxy." + key.toLowerCase(Locale.ROOT));
    }

    public Map<String, String> staffGroups() {
        Map<String, Object> section = yaml.getSection("staff.groups");
        Map<String, String> groups = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : section.entrySet()) {
            groups.put(entry.getKey().toLowerCase(Locale.ROOT), String.valueOf(entry.getValue()));
        }

        if (groups.isEmpty()) {
            groups.put("admin", permission("staff-admin"));
            groups.put("mod", permission("staff-mod"));
            groups.put("helper", permission("staff-helper"));
        }

        return groups;
    }

    public Map<String, String> all() {
        return Map.copyOf(permissionByKey);
    }

    private static String normalize(String key) {
        return key.replace('_', '-').toLowerCase(Locale.ROOT);
    }
}
