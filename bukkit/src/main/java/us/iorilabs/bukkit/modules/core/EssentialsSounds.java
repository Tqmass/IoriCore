package us.iorilabs.bukkit.modules.core;

import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.fx.SoundService;

public final class EssentialsSounds {

    private EssentialsSounds() {}

    public static void play(IoriCorePlugin plugin, Player player, String logicalId) {
        if (plugin == null || player == null || logicalId == null || logicalId.isEmpty()) return;
        SoundService sounds = plugin.sounds();
        if (sounds == null) return;
        sounds.play(player, logicalId);
    }
}
