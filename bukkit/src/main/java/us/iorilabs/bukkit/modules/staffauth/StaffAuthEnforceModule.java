package us.iorilabs.bukkit.modules.staffauth;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.HandlerList;
import us.iorilabs.api.modules.ModuleId;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.commands.BukkitCommandBridge;
import us.iorilabs.bukkit.modules.CoreModule;
import us.iorilabs.bukkit.modules.ModuleToggle;
import us.iorilabs.bukkit.modules.staffauth.command.StaffRegCommand;

@Getter
@Accessors(fluent = true)
public final class StaffAuthEnforceModule implements CoreModule {

    private final IoriCorePlugin plugin;
    private StaffAuthEnforceService service;
    private StaffAuthEnforceListener listener;
    private StaffRegService staffReg;

    public StaffAuthEnforceModule(IoriCorePlugin plugin, FileConfiguration settings) {
        this.plugin = plugin;
    }

    @Override
    public ModuleId id() {
        return ModuleId.STAFF_AUTH;
    }

    @Override
    public boolean enabled() {
        return ModuleToggle.enabled(plugin.settings(), ModuleId.STAFF_AUTH);
    }

    public static void registerCommands(IoriCorePlugin plugin, FileConfiguration settings) {
        StaffAuthEnforceModule module = plugin.modules() == null ? null : plugin.modules().get(ModuleId.STAFF_AUTH);
        StaffRegService reg = module == null ? null : module.staffReg;
        if (reg == null) {
            reg = newStaffReg(plugin, settings);
            if (module != null) {
                module.staffReg = reg;
                if (module.service != null) module.service.bindStaffReg(reg);
            }
        } else {
            reg.refreshFromSettings(settings);
        }
        BukkitCommandBridge.register(plugin, new StaffRegCommand(plugin.language(), reg));
    }

    @Override
    public void enable() {
        FileConfiguration settings = plugin.settings();
        staffReg = newStaffReg(plugin, settings);
        String permission = staffPermission(plugin, settings);
        int graceTicks = Math.max(20, settings.getInt("STAFF-AUTH.GRACE-TICKS", 40));
        service = new StaffAuthEnforceService(plugin, permission, graceTicks);
        service.bindStaffReg(staffReg);
        listener = new StaffAuthEnforceListener(service);
        Bukkit.getPluginManager().registerEvents(listener, plugin);
    }

    @Override
    public void reload() {
        FileConfiguration settings = plugin.settings();
        if (staffReg == null) {
            staffReg = newStaffReg(plugin, settings);
        } else {
            staffReg.refreshFromSettings(settings);
        }
        if (service != null) service.bindStaffReg(staffReg);
    }

    @Override
    public void disable() {
        if (listener != null) {
            HandlerList.unregisterAll(listener);
            listener = null;
        }
        if (service != null) {
            service.clearAll();
            service = null;
        }
        staffReg = null;
    }

    private static StaffRegService newStaffReg(IoriCorePlugin plugin, FileConfiguration settings) {
        return new StaffRegService(
            plugin,
            staffPermission(plugin, settings),
            settings.getInt("STAFF-AUTH.REG-CODE-TTL-SECONDS", 900)
        );
    }

    private static String staffPermission(IoriCorePlugin plugin, FileConfiguration settings) {
        String node =
            plugin.permissions() == null ? null : plugin.permissions().getString("STAFF-AUTH.STAFF-PERMISSION");
        if (node == null || node.isBlank()) node = settings.getString("STAFF-AUTH.STAFF-PERMISSION", "ioriproxy.staff");
        return node == null || node.isBlank() ? "ioriproxy.staff" : node.trim();
    }
}
