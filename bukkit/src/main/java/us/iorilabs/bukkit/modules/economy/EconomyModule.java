package us.iorilabs.bukkit.modules.economy;

import lombok.Getter;
import lombok.experimental.Accessors;
import us.iorilabs.api.modules.ModuleId;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.commands.BukkitCommandBridge;
import us.iorilabs.bukkit.modules.CoreModule;
import us.iorilabs.bukkit.modules.economy.command.BalanceCommand;
import us.iorilabs.bukkit.modules.economy.command.EcoCommand;
import us.iorilabs.bukkit.modules.economy.command.PayCommand;

@Getter
@Accessors(fluent = true)
public final class EconomyModule implements CoreModule {

    private final IoriCorePlugin plugin;
    private final boolean enabledFlag;
    private EconomyService service;

    public EconomyModule(IoriCorePlugin plugin) {
        this.plugin = plugin;
        this.enabledFlag = plugin.settings().getBoolean("MODULES.ECONOMY", true);
    }

    @Override
    public ModuleId id() {
        return ModuleId.ECONOMY;
    }

    @Override
    public boolean enabled() {
        return enabledFlag;
    }

    @Override
    public void enable() {
        service = new EconomyService(plugin);
        BukkitCommandBridge.register(plugin, new BalanceCommand(plugin, service));
        BukkitCommandBridge.register(plugin, new PayCommand(plugin, service));
        BukkitCommandBridge.register(plugin, new EcoCommand(plugin, service));
    }

    @Override
    public void disable() {
        service = null;
    }
}
