package us.iorilabs.bukkit.messaging;

import org.bukkit.configuration.file.FileConfiguration;
import us.iorilabs.api.modules.ModuleId;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.modules.CoreModule;

public final class PluginMessagingModule implements CoreModule {

    private final IoriCorePlugin plugin;
    private final boolean enabledFlag;
    private PluginMessagingBridge bridge;

    public PluginMessagingModule(IoriCorePlugin plugin, FileConfiguration settings) {
        this.plugin = plugin;
        this.enabledFlag = settings.getBoolean("MODULES.PLUGIN-MESSAGING", true);
    }

    @Override
    public ModuleId id() {
        return ModuleId.PLUGIN_MESSAGING;
    }

    @Override
    public boolean enabled() {
        return enabledFlag;
    }

    @Override
    public void enable() {
        bridge = new PluginMessagingBridge(plugin);
        bridge.enable();
    }

    @Override
    public void disable() {
        if (bridge != null) {
            bridge.disable();
            bridge = null;
        }
    }
}
