package us.iorilabs.bukkit.punishment;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import us.iorilabs.api.hook.LiteBansHook;
import us.iorilabs.api.hook.NoopLiteBansHook;
import us.iorilabs.api.modules.ModuleId;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.commands.BukkitCommandBridge;
import us.iorilabs.bukkit.hook.SoftLiteBansHook;
import us.iorilabs.bukkit.modules.CoreModule;
import us.iorilabs.bukkit.punishment.command.BanCommand;
import us.iorilabs.bukkit.punishment.command.KickCommand;
import us.iorilabs.bukkit.punishment.command.MuteCommand;
import us.iorilabs.bukkit.punishment.command.PunishCommand;
import us.iorilabs.bukkit.punishment.command.UnbanCommand;
import us.iorilabs.bukkit.punishment.command.UnmuteCommand;
import us.iorilabs.bukkit.punishment.command.UnwarnCommand;
import us.iorilabs.bukkit.punishment.command.WarnCommand;

@Getter
@Accessors(fluent = true)
public final class PunishmentsModule implements CoreModule {

    private final IoriCorePlugin plugin;
    private final boolean enabledFlag;
    private final boolean liteBansFlag;
    private PunishmentConfig config;
    private PunishmentService punishments;
    private PunishReasonService reasons;
    private LiteBansHook liteBans = new NoopLiteBansHook();
    private MuteListener muteListener;
    private BanListener banListener;

    public PunishmentsModule(IoriCorePlugin plugin) {
        this.plugin = plugin;
        this.enabledFlag = plugin.settings().getBoolean("MODULES.PUNISHMENTS", true);
        this.liteBansFlag = plugin.settings().getBoolean("MODULES.LITEBANS-HOOK", true);
    }

    @Override
    public ModuleId id() {
        return ModuleId.PUNISHMENTS;
    }

    @Override
    public boolean enabled() {
        return enabledFlag;
    }

    @Override
    public void enable() {
        config = PunishmentConfig.load(plugin);
        punishments = new PunishmentService(plugin, plugin.storage(), plugin.getLogger());
        reasons = new PunishReasonService(plugin, config);
        liteBans = liteBansFlag ? new SoftLiteBansHook(plugin.getLogger(), true) : new NoopLiteBansHook();
        punishments.start(liteBans);

        BukkitCommandBridge.register(plugin, new PunishCommand(plugin));
        BukkitCommandBridge.register(plugin, new BanCommand(plugin.language(), punishments));
        BukkitCommandBridge.register(plugin, new MuteCommand(plugin.language(), punishments));
        BukkitCommandBridge.register(plugin, new WarnCommand(plugin.language(), punishments));
        BukkitCommandBridge.register(plugin, new KickCommand(plugin.language(), punishments));
        BukkitCommandBridge.register(plugin, new UnbanCommand(plugin.language(), punishments));
        BukkitCommandBridge.register(plugin, new UnmuteCommand(plugin.language(), punishments));
        BukkitCommandBridge.register(plugin, new UnwarnCommand(plugin.language(), punishments));

        muteListener = new MuteListener(punishments);
        banListener = new BanListener(plugin, punishments);
        Bukkit.getPluginManager().registerEvents(muteListener, plugin);
        Bukkit.getPluginManager().registerEvents(banListener, plugin);
    }

    public static boolean blockIfMuted(IoriCorePlugin plugin, Player player) {
        if (plugin.modules() == null || player == null) return false;
        CoreModule module = plugin.modules().get(ModuleId.PUNISHMENTS);
        if (!(module instanceof PunishmentsModule) || !module.enabled()) return false;
        PunishmentService service = ((PunishmentsModule) module).punishments();
        return service != null && service.blockIfMuted(player);
    }

    @Override
    public void disable() {
        if (muteListener != null) HandlerList.unregisterAll(muteListener);
        muteListener = null;
        if (banListener != null) HandlerList.unregisterAll(banListener);
        banListener = null;
        if (punishments != null) {
            punishments.shutdown();
            punishments = null;
        }
        reasons = null;
        config = null;
        liteBans = new NoopLiteBansHook();
    }
}
