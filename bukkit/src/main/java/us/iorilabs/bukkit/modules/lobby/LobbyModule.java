package us.iorilabs.bukkit.modules.lobby;

import java.util.List;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.HandlerList;
import us.iorilabs.api.modules.ModuleId;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.commands.BukkitCommandBridge;
import us.iorilabs.bukkit.modules.CoreModule;
import us.iorilabs.bukkit.modules.lobby.command.GamesCommand;
import us.iorilabs.bukkit.modules.lobby.command.GlowCommand;
import us.iorilabs.bukkit.util.DisplayYaml;

@Getter
@Accessors(fluent = true)
public final class LobbyModule implements CoreModule {

    private final IoriCorePlugin plugin;
    private final boolean enabledFlag;
    private GroupsService groups;
    private LobbyHotbarService hotbar;
    private LobbyGlowService glow;
    private LobbyCtwMenus menus;
    private LobbyHotbarListener hotbarListener;

    public LobbyModule(IoriCorePlugin plugin) {
        this.plugin = plugin;
        this.enabledFlag = plugin.settings().getBoolean("MODULES.LOBBY", true);
    }

    @Override
    public ModuleId id() {
        return ModuleId.LOBBY;
    }

    @Override
    public boolean enabled() {
        return enabledFlag;
    }

    @Override
    public void enable() {
        FileConfiguration tab = DisplayYaml.load(plugin, "tab.yml");
        List<String> npcPrefixes = tab.getStringList("NPC-NAME-PREFIXES");
        groups = new GroupsService(plugin, npcPrefixes);
        groups.start();
        hotbar = new LobbyHotbarService(plugin);
        glow = new LobbyGlowService(plugin);
        menus = new LobbyCtwMenus(plugin, glow);
        BukkitCommandBridge.register(plugin, new GlowCommand(plugin, menus));
        BukkitCommandBridge.register(plugin, new GamesCommand(plugin, menus));
        hotbarListener = new LobbyHotbarListener(hotbar);
        Bukkit.getPluginManager().registerEvents(hotbarListener, plugin);
    }

    @Override
    public void disable() {
        if (hotbarListener != null) {
            HandlerList.unregisterAll(hotbarListener);
            hotbarListener = null;
        }
        if (groups != null) {
            groups.stop();
            groups = null;
        }
        hotbar = null;
        glow = null;
        menus = null;
    }
}
