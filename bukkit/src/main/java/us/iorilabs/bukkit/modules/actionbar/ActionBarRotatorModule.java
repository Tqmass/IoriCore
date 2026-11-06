package us.iorilabs.bukkit.modules.actionbar;

import lombok.Getter;
import lombok.experimental.Accessors;
import us.iorilabs.api.modules.ModuleId;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.modules.CoreModule;

@Getter
@Accessors(fluent = true)
public final class ActionBarRotatorModule implements CoreModule {

    private final IoriCorePlugin plugin;
    private final boolean enabledFlag;
    private ActionBarRotatorService service;

    public ActionBarRotatorModule(IoriCorePlugin plugin) {
        this.plugin = plugin;
        boolean moduleOn = plugin.settings().getBoolean("MODULES.ACTION-BAR", true);
        boolean settingsOn = plugin.settings().getBoolean("ACTION-BAR.ENABLED", true);
        this.enabledFlag = moduleOn && settingsOn;
    }

    @Override
    public ModuleId id() {
        return ModuleId.ACTION_BAR;
    }

    @Override
    public boolean enabled() {
        return enabledFlag;
    }

    @Override
    public void enable() {
        service = new ActionBarRotatorService(plugin);
        service.start();
    }

    @Override
    public void disable() {
        if (service != null) {
            service.stop();
            service = null;
        }
    }

    @Override
    public void reload() {
        disable();
        enable();
    }
}
