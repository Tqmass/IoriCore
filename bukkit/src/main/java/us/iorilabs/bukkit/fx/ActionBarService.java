package us.iorilabs.bukkit.fx;

import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import us.iorilabs.abstraction.VersionWrapper;
import us.iorilabs.bukkit.IoriCorePlugin;

public final class ActionBarService {

    private final IoriCorePlugin plugin;
    private final SoundService sounds;

    public ActionBarService(IoriCorePlugin plugin, SoundService sounds) {
        this.plugin = plugin;
        this.sounds = sounds;
    }

    public void send(Player player, String message) {
        send(player, message, null);
    }

    public void send(Player player, String message, SoundKey sound) {
        if (player == null || !player.isOnline()) return;
        VersionWrapper wrapper = plugin.abstraction();
        if (wrapper == null) return;

        String colored = color(message);
        if (colored.isEmpty()) return;
        wrapper.sendActionBar(player.getUniqueId(), colored);
        if (sound != null) sounds.play(player, sound);
    }

    public void send(UUID playerId, String message) {
        if (playerId == null) return;
        VersionWrapper wrapper = plugin.abstraction();
        if (wrapper == null) return;
        String colored = color(message);
        if (colored.isEmpty()) return;
        wrapper.sendActionBar(playerId, colored);
    }

    private static String color(String input) {
        if (input == null) return "";
        return ChatColor.translateAlternateColorCodes('&', input);
    }
}
