package us.iorilabs.bukkit.menu.api.yaml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class YamlPlaceholders {

    private YamlPlaceholders() {}

    public static Map<String, String> base(Player player, Map<String, String> ctx) {
        Map<String, String> out = new HashMap<>();
        if (ctx != null) out.putAll(ctx);
        if (player != null) {
            out.putIfAbsent("player", player.getName());
            out.putIfAbsent("uuid", player.getUniqueId().toString());
            out.putIfAbsent("world", player.getWorld() == null ? "" : player.getWorld().getName());
        }
        return out;
    }

    public static String apply(String input, Player player, Map<String, String> placeholders) {
        if (input == null || input.isEmpty()) return "";
        String out = input;
        Map<String, String> values = placeholders == null ? Collections.emptyMap() : placeholders;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (entry.getKey() == null) continue;
            String value = entry.getValue() == null ? "" : entry.getValue();
            out = out.replace("{" + entry.getKey() + "}", value);
            out = out.replace("%" + entry.getKey() + "%", value);
        }
        return softPapi(player, out);
    }

    public static List<String> apply(List<String> lines, Player player, Map<String, String> placeholders) {
        if (lines == null || lines.isEmpty()) return Collections.emptyList();
        List<String> out = new ArrayList<>(lines.size());
        for (String line : lines) out.add(apply(line, player, placeholders));
        return out;
    }

    public static String firstToken(String argument) {
        if (argument == null) return "";
        String trimmed = argument.trim();
        if (trimmed.isEmpty()) return "";
        int space = trimmed.indexOf(' ');
        return space < 0 ? trimmed : trimmed.substring(0, space).trim();
    }

    public static String rest(String argument) {
        if (argument == null) return "";
        String trimmed = argument.trim();
        int space = trimmed.indexOf(' ');
        return space < 0 ? "" : trimmed.substring(space + 1).trim();
    }

    public static Map<String, String> with(Map<String, String> base, String key, String value) {
        Map<String, String> out = new HashMap<>();
        if (base != null) out.putAll(base);
        if (key != null) out.put(key, value == null ? "" : value);
        return out;
    }

    public static Map<String, String> merge(Map<String, String> left, Map<String, String> right) {
        Map<String, String> out = new HashMap<>();
        if (left != null) out.putAll(left);
        if (right != null) out.putAll(right);
        return out;
    }

    private static String softPapi(Player player, String text) {
        if (player == null || text == null || text.isEmpty()) return text == null ? "" : text;
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) return text;
        try {
            Class<?> papi = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            Object result = papi.getMethod("setPlaceholders", Player.class, String.class).invoke(null, player, text);
            return result == null ? text : String.valueOf(result);
        } catch (ReflectiveOperationException ignored) {
            return text;
        }
    }

    public static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
