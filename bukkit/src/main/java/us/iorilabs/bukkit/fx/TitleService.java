package us.iorilabs.bukkit.fx;

import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import us.iorilabs.abstraction.VersionWrapper;
import us.iorilabs.bukkit.IoriCorePlugin;

public final class TitleService {

    private static final int DEFAULT_FADE_IN = 10;
    private static final int DEFAULT_STAY = 40;
    private static final int DEFAULT_FADE_OUT = 10;

    private final IoriCorePlugin plugin;
    private final SoundService sounds;

    public TitleService(IoriCorePlugin plugin, SoundService sounds) {
        this.plugin = plugin;
        this.sounds = sounds;
    }

    public void send(Player player, String title, String subtitle) {
        send(player, title, subtitle, DEFAULT_FADE_IN, DEFAULT_STAY, DEFAULT_FADE_OUT, null);
    }

    public void send(Player player, String title, String subtitle, SoundKey sound) {
        send(player, title, subtitle, DEFAULT_FADE_IN, DEFAULT_STAY, DEFAULT_FADE_OUT, sound);
    }

    public void send(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        send(player, title, subtitle, fadeIn, stay, fadeOut, null);
    }

    public void send(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut, SoundKey sound) {
        if (player == null || !player.isOnline()) return;
        VersionWrapper wrapper = plugin.abstraction();
        if (wrapper == null) return;

        wrapper.sendLegacyTitle(player.getUniqueId(), color(title), color(subtitle), fadeIn, stay, fadeOut);
        if (sound != null) sounds.play(player, sound);
    }

    public void send(UUID playerId, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        if (playerId == null) return;
        VersionWrapper wrapper = plugin.abstraction();
        if (wrapper == null) return;
        wrapper.sendLegacyTitle(playerId, color(title), color(subtitle), fadeIn, stay, fadeOut);
    }

    private static String color(String input) {
        if (input == null) return "";
        return ChatColor.translateAlternateColorCodes('&', input);
    }
}
