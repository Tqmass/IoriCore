package us.iorilabs.bukkit.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class PlaceholderApiBridge {

    private PlaceholderApiBridge() {}

    public static String set(Player player, String text) {
        if (player == null || text == null || text.isEmpty() || !text.contains("%")) return text == null ? "" : text;
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) return text;
        try {
            Class<?> papi = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            Object result = papi.getMethod("setPlaceholders", Player.class, String.class).invoke(null, player, text);
            return result == null ? text : String.valueOf(result);
        } catch (ReflectiveOperationException ignored) {
            return text;
        }
    }
}
