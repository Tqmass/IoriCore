package us.iorilabs.bukkit.modules.core.vanish;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import us.iorilabs.api.modules.ModuleId;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.modules.core.CoreFeaturesModule;

public final class VanishVisibility {

    private VanishVisibility() {}

    public static VanishService service(IoriCorePlugin plugin) {
        if (plugin == null || plugin.modules() == null) return null;
        CoreFeaturesModule module = plugin.modules().get(ModuleId.CORE);
        return module == null ? null : module.vanish();
    }

    public static boolean vanished(IoriCorePlugin plugin, UUID uuid) {
        VanishService vanish = service(plugin);
        return vanish != null && vanish.vanished(uuid);
    }

    public static boolean canSee(CommandSender viewer, Player target) {
        if (target == null) return false;
        if (!(viewer instanceof Player)) return true;
        Player player = (Player) viewer;
        if (player.getUniqueId().equals(target.getUniqueId())) return true;
        if (!vanished(pluginOf(player), target.getUniqueId())) return true;
        return (
            player.hasPermission("ioricore.vanish.see") ||
            player.hasPermission("ioricore.staff") ||
            player.hasPermission("ioricore.admin")
        );
    }

    public static boolean canSee(IoriCorePlugin plugin, Player viewer, Player target) {
        if (target == null) return false;
        if (viewer == null) return true;
        if (viewer.getUniqueId().equals(target.getUniqueId())) return true;
        if (!vanished(plugin, target.getUniqueId())) return true;
        return (
            viewer.hasPermission("ioricore.vanish.see") ||
            viewer.hasPermission("ioricore.staff") ||
            viewer.hasPermission("ioricore.admin")
        );
    }

    public static int visibleOnlineCount(IoriCorePlugin plugin, Player viewer) {
        int count = 0;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (canSee(plugin, viewer, online)) count++;
        }
        return count;
    }

    public static List<Player> visibleOnline(IoriCorePlugin plugin, Player viewer) {
        List<Player> out = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (canSee(plugin, viewer, online)) out.add(online);
        }
        return out;
    }

    public static Collection<? extends Player> filter(
        IoriCorePlugin plugin,
        Player viewer,
        Collection<? extends Player> source
    ) {
        if (source == null || source.isEmpty()) return source;
        List<Player> out = new ArrayList<>(source.size());
        for (Player online : source) {
            if (canSee(plugin, viewer, online)) out.add(online);
        }
        return out;
    }

    private static IoriCorePlugin pluginOf(Player player) {
        if (player == null) return null;
        org.bukkit.plugin.Plugin plugin = Bukkit.getPluginManager().getPlugin("IoriCore");
        return plugin instanceof IoriCorePlugin ? (IoriCorePlugin) plugin : null;
    }
}
