package us.iorilabs.bukkit.modules.lobby;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;
import us.iorilabs.api.hook.ApolloHook;
import us.iorilabs.bukkit.IoriCorePlugin;

public final class LobbyGlowService {

    private final IoriCorePlugin plugin;
    private final Map<UUID, String> selected = new ConcurrentHashMap<>();

    public LobbyGlowService(IoriCorePlugin plugin) {
        this.plugin = plugin;
    }

    public String selectedKey(Player player) {
        if (player == null) return null;
        return selected.get(player.getUniqueId());
    }

    public void setSelected(Player player, LobbyGlowColor color) {
        if (player == null || color == null) return;
        selected.put(player.getUniqueId(), color.key());
        apply(player, color);
    }

    public void reset(Player player) {
        if (player == null) return;
        selected.remove(player.getUniqueId());
        ApolloHook apollo = plugin.apolloHook();
        if (apollo != null && apollo.available()) apollo.resetGlow(player.getUniqueId());
    }

    public void apply(Player player, LobbyGlowColor color) {
        if (player == null || color == null) return;
        ApolloHook apollo = plugin.apolloHook();
        if (apollo == null || !apollo.available()) return;
        apollo.overrideGlow(player.getUniqueId(), color.toColor().getRGB() & 0xFFFFFF);
    }

    public boolean hasColor(Player player, String colorKey) {
        if (player == null || colorKey == null) return false;
        if (player.hasPermission("ioricore.glow.*")) return true;
        return player.hasPermission("ioricore.glow." + colorKey.toLowerCase());
    }

    public void clear(Player player) {
        if (player == null) return;
        selected.remove(player.getUniqueId());
    }
}
