package us.iorilabs.bukkit.menu.api.yaml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.menu.enums.RequirementKind;

@Getter
@Accessors(fluent = true)
public final class YamlRequirement {

    private final RequirementKind kind;
    private final String permission;
    private final String input;
    private final String output;
    private final String compare;
    private final boolean expected;

    public YamlRequirement(
        RequirementKind kind,
        String permission,
        String input,
        String output,
        String compare,
        boolean expected
    ) {
        this.kind = kind == null ? RequirementKind.BOOLEAN : kind;
        this.permission = permission == null ? "" : permission;
        this.input = input == null ? "" : input;
        this.output = output == null ? "" : output;
        this.compare = compare == null ? "equals" : compare;
        this.expected = expected;
    }

    public static List<YamlRequirement> parseList(List<?> raw) {
        if (raw == null || raw.isEmpty()) return Collections.emptyList();
        List<YamlRequirement> out = new ArrayList<>();
        for (Object entry : raw) {
            YamlRequirement requirement = parse(entry);
            if (requirement != null) out.add(requirement);
        }
        return out;
    }

    public static YamlRequirement parse(Object raw) {
        if (raw == null) return null;
        if (raw instanceof String) {
            String line = ((String) raw).trim();
            if (line.isEmpty()) return null;
            if (line.startsWith("[permission]")) {
                return new YamlRequirement(
                    RequirementKind.PERMISSION,
                    line.substring("[permission]".length()).trim(),
                    "",
                    "",
                    "equals",
                    true
                );
            }
            if (line.startsWith("permission:")) {
                return new YamlRequirement(
                    RequirementKind.PERMISSION,
                    line.substring("permission:".length()).trim(),
                    "",
                    "",
                    "equals",
                    true
                );
            }
            return new YamlRequirement(RequirementKind.BOOLEAN, "", line, "true", "equals", true);
        }
        if (!(raw instanceof ConfigurationSection) && !(raw instanceof Map)) return null;

        ConfigurationSection section =
            raw instanceof ConfigurationSection ? (ConfigurationSection) raw : mapSection((Map<?, ?>) raw);
        String type = section.getString("TYPE", section.getString("type", "permission"));
        RequirementKind kind = kindOf(type);
        return new YamlRequirement(
            kind,
            section.getString("PERMISSION", section.getString("permission", "")),
            section.getString("INPUT", section.getString("input", "")),
            section.getString("OUTPUT", section.getString("output", section.getString("VALUE", "true"))),
            section.getString("COMPARE", section.getString("compare", "equals")),
            section.getBoolean("EXPECTED", section.getBoolean("expected", true))
        );
    }

    public boolean test(Player player, Map<String, String> placeholders) {
        if (player == null) return false;
        switch (kind) {
            case PERMISSION:
                String node = YamlPlaceholders.apply(permission, player, placeholders);
                return player.hasPermission(node) || player.hasPermission("ioricore.admin");
            case COMPARE:
                String left = YamlPlaceholders.apply(input, player, placeholders);
                String right = YamlPlaceholders.apply(output, player, placeholders);
                return matches(left, right) == expected;
            case BOOLEAN:
            default:
                String value = YamlPlaceholders.apply(input.isEmpty() ? output : input, player, placeholders);
                boolean parsed = "true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value) || "1".equals(value);
                return parsed == expected;
        }
    }

    public static boolean all(List<YamlRequirement> requirements, Player player, Map<String, String> placeholders) {
        if (requirements == null || requirements.isEmpty()) return true;
        for (YamlRequirement requirement : requirements) {
            if (requirement == null) continue;
            if (!requirement.test(player, placeholders)) return false;
        }
        return true;
    }

    private boolean matches(String left, String right) {
        String mode = compare.trim().toLowerCase(Locale.ROOT);
        if ("contains".equals(mode)) return left.contains(right);
        if ("starts".equals(mode) || "startswith".equals(mode)) return left.startsWith(right);
        if ("not".equals(mode) || "notequals".equals(mode) || "!=".equals(mode)) return !left.equalsIgnoreCase(right);
        return left.equalsIgnoreCase(right);
    }

    private static RequirementKind kindOf(String type) {
        if (type == null) return RequirementKind.PERMISSION;
        String key = type.trim().toLowerCase(Locale.ROOT);
        if (key.contains("compare") || key.contains("equals") || key.contains("string")) return RequirementKind.COMPARE;
        if (key.contains("bool")) return RequirementKind.BOOLEAN;
        return RequirementKind.PERMISSION;
    }

    @SuppressWarnings("unchecked")
    private static ConfigurationSection mapSection(Map<?, ?> map) {
        org.bukkit.configuration.file.YamlConfiguration yaml = new org.bukkit.configuration.file.YamlConfiguration();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() == null) continue;
            yaml.set(String.valueOf(entry.getKey()), entry.getValue());
        }
        return yaml;
    }
}
