package us.iorilabs.bukkit.modules.core;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import us.iorilabs.api.modules.ModuleId;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.commands.BukkitCommandBridge;
import us.iorilabs.bukkit.config.CoreFeatureSettings;
import us.iorilabs.bukkit.modules.CoreModule;
import us.iorilabs.bukkit.modules.ModuleToggle;
import us.iorilabs.bukkit.modules.core.command.BackCommand;
import us.iorilabs.bukkit.modules.core.command.BroadcastCommand;
import us.iorilabs.bukkit.modules.core.command.ClearCommand;
import us.iorilabs.bukkit.modules.core.command.CreateKitCommand;
import us.iorilabs.bukkit.modules.core.command.DelHomeCommand;
import us.iorilabs.bukkit.modules.core.command.DelWarpCommand;
import us.iorilabs.bukkit.modules.core.command.DeleteKitCommand;
import us.iorilabs.bukkit.modules.core.command.DisposalCommand;
import us.iorilabs.bukkit.modules.core.command.EnderchestCommand;
import us.iorilabs.bukkit.modules.core.command.FeedCommand;
import us.iorilabs.bukkit.modules.core.command.FlyCommand;
import us.iorilabs.bukkit.modules.core.command.GamemodeCommand;
import us.iorilabs.bukkit.modules.core.command.GetPosCommand;
import us.iorilabs.bukkit.modules.core.command.GiveCommand;
import us.iorilabs.bukkit.modules.core.command.GodCommand;
import us.iorilabs.bukkit.modules.core.command.HatCommand;
import us.iorilabs.bukkit.modules.core.command.HealAllCommand;
import us.iorilabs.bukkit.modules.core.command.HealCommand;
import us.iorilabs.bukkit.modules.core.command.HomeCommand;
import us.iorilabs.bukkit.modules.core.command.HomesCommand;
import us.iorilabs.bukkit.modules.core.command.IgnoreCommand;
import us.iorilabs.bukkit.modules.core.command.InvseeCommand;
import us.iorilabs.bukkit.modules.core.command.KillCommand;
import us.iorilabs.bukkit.modules.core.command.KitCommand;
import us.iorilabs.bukkit.modules.core.command.MeCommand;
import us.iorilabs.bukkit.modules.core.command.MsgCommand;
import us.iorilabs.bukkit.modules.core.command.NearCommand;
import us.iorilabs.bukkit.modules.core.command.NickCommand;
import us.iorilabs.bukkit.modules.core.command.PingCommand;
import us.iorilabs.bukkit.modules.core.command.RealnameCommand;
import us.iorilabs.bukkit.modules.core.command.RepairCommand;
import us.iorilabs.bukkit.modules.core.command.ReplyCommand;
import us.iorilabs.bukkit.modules.core.command.SetHomeCommand;
import us.iorilabs.bukkit.modules.core.command.SetSpawnCommand;
import us.iorilabs.bukkit.modules.core.command.SetWarpCommand;
import us.iorilabs.bukkit.modules.core.command.SkullCommand;
import us.iorilabs.bukkit.modules.core.command.SpawnCommand;
import us.iorilabs.bukkit.modules.core.command.SpeedCommand;
import us.iorilabs.bukkit.modules.core.command.TimeCommand;
import us.iorilabs.bukkit.modules.core.command.TopCommand;
import us.iorilabs.bukkit.modules.core.command.TpAcceptCommand;
import us.iorilabs.bukkit.modules.core.command.TpAllCommand;
import us.iorilabs.bukkit.modules.core.command.TpCancelCommand;
import us.iorilabs.bukkit.modules.core.command.TpCommand;
import us.iorilabs.bukkit.modules.core.command.TpDenyCommand;
import us.iorilabs.bukkit.modules.core.command.TpHereCommand;
import us.iorilabs.bukkit.modules.core.command.TpPosCommand;
import us.iorilabs.bukkit.modules.core.command.TpaCommand;
import us.iorilabs.bukkit.modules.core.command.TpaHereCommand;
import us.iorilabs.bukkit.modules.core.command.VanishCommand;
import us.iorilabs.bukkit.modules.core.command.WarpCommand;
import us.iorilabs.bukkit.modules.core.command.WeatherCommand;
import us.iorilabs.bukkit.modules.core.command.WhitelistCommand;
import us.iorilabs.bukkit.modules.core.command.WorkbenchCommand;
import us.iorilabs.bukkit.modules.core.command.WorldCommand;
import us.iorilabs.bukkit.modules.core.home.HomeService;
import us.iorilabs.bukkit.modules.core.ignore.IgnoreService;
import us.iorilabs.bukkit.modules.core.kit.KitService;
import us.iorilabs.bukkit.modules.core.spawn.SpawnService;
import us.iorilabs.bukkit.modules.core.tpa.TpaService;
import us.iorilabs.bukkit.modules.core.vanish.VanishService;
import us.iorilabs.bukkit.modules.core.warp.WarpService;

@Getter
@Accessors(fluent = true)
public final class CoreFeaturesModule implements CoreModule {

    private final IoriCorePlugin plugin;
    private EssentialsState state;
    private SpawnService spawn;
    private HomeService homes;
    private WarpService warps;
    private TpaService tpa;
    private VanishService vanish;
    private KitService kits;
    private IgnoreService ignore;
    private EssentialsListener essentialsListener;
    private WorldManagementListener worldListener;

    public CoreFeaturesModule(IoriCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public ModuleId id() {
        return ModuleId.CORE;
    }

    @Override
    public boolean enabled() {
        return ModuleToggle.enabled(plugin.settings(), ModuleId.CORE);
    }

    @Override
    public void enable() {
        state = new EssentialsState(plugin);
        spawn = new SpawnService(plugin);
        homes = new HomeService(plugin);
        warps = new WarpService(plugin);
        tpa = new TpaService(plugin);
        vanish = new VanishService(plugin);
        kits = new KitService(plugin);
        ignore = new IgnoreService(plugin);

        registerCommands();

        essentialsListener = new EssentialsListener(
            state,
            spawn,
            homes,
            tpa,
            vanish,
            CoreFeatureSettings.getBoolean(plugin.settings(), "SPAWN-ON-JOIN", false),
            CoreFeatureSettings.getBoolean(plugin.settings(), "SPAWN-ON-DEATH", true)
        );
        worldListener = new WorldManagementListener(plugin.settings());
        Bukkit.getPluginManager().registerEvents(essentialsListener, plugin);
        Bukkit.getPluginManager().registerEvents(worldListener, plugin);
    }

    @Override
    public void disable() {
        if (essentialsListener != null) {
            HandlerList.unregisterAll(essentialsListener);
            essentialsListener = null;
        }
        if (worldListener != null) {
            HandlerList.unregisterAll(worldListener);
            worldListener = null;
        }
        ignore = null;
        kits = null;
        vanish = null;
        tpa = null;
        warps = null;
        homes = null;
        spawn = null;
        state = null;
    }

    private void registerCommands() {
        BukkitCommandBridge.register(plugin, new HealCommand(plugin));
        BukkitCommandBridge.register(plugin, new HealAllCommand(plugin));
        BukkitCommandBridge.register(plugin, new FeedCommand(plugin));
        BukkitCommandBridge.register(plugin, new FlyCommand(plugin));
        BukkitCommandBridge.register(plugin, new GodCommand(plugin, state));
        BukkitCommandBridge.register(plugin, new GamemodeCommand(plugin));
        BukkitCommandBridge.register(plugin, new SpeedCommand(plugin));
        BukkitCommandBridge.register(plugin, new KillCommand(plugin));
        BukkitCommandBridge.register(plugin, new WeatherCommand(plugin));
        BukkitCommandBridge.register(plugin, new TimeCommand(plugin));
        BukkitCommandBridge.register(plugin, new TopCommand(plugin, state));
        BukkitCommandBridge.register(plugin, new WorldCommand(plugin, state));
        BukkitCommandBridge.register(plugin, new TpCommand(plugin, state));
        BukkitCommandBridge.register(plugin, new TpHereCommand(plugin, state));
        BukkitCommandBridge.register(plugin, new TpAllCommand(plugin, state, vanish));
        BukkitCommandBridge.register(plugin, new TpPosCommand(plugin, state));
        BukkitCommandBridge.register(plugin, new BackCommand(plugin, state));
        BukkitCommandBridge.register(plugin, new NickCommand(plugin, state));
        BukkitCommandBridge.register(plugin, new RealnameCommand(plugin, state));
        BukkitCommandBridge.register(plugin, new RepairCommand(plugin));
        BukkitCommandBridge.register(plugin, new EnderchestCommand(plugin));
        BukkitCommandBridge.register(plugin, new InvseeCommand(plugin));
        BukkitCommandBridge.register(plugin, new ClearCommand(plugin));
        BukkitCommandBridge.register(plugin, new BroadcastCommand(plugin));
        BukkitCommandBridge.register(plugin, new WhitelistCommand(plugin));
        BukkitCommandBridge.register(plugin, new GiveCommand(plugin));
        BukkitCommandBridge.register(plugin, new GetPosCommand(plugin));
        BukkitCommandBridge.register(plugin, new DisposalCommand(plugin));
        BukkitCommandBridge.register(plugin, new WorkbenchCommand(plugin));
        BukkitCommandBridge.register(plugin, new HatCommand(plugin));
        BukkitCommandBridge.register(plugin, new NearCommand(plugin));
        BukkitCommandBridge.register(plugin, new PingCommand(plugin));
        BukkitCommandBridge.register(plugin, new MeCommand(plugin));
        BukkitCommandBridge.register(plugin, new SkullCommand(plugin));
        BukkitCommandBridge.register(plugin, new IgnoreCommand(plugin, ignore));
        BukkitCommandBridge.register(plugin, new VanishCommand(plugin, vanish));
        BukkitCommandBridge.register(plugin, new SpawnCommand(plugin, spawn, state));
        BukkitCommandBridge.register(plugin, new SetSpawnCommand(plugin, spawn));
        BukkitCommandBridge.register(plugin, new HomeCommand(plugin, homes, state));
        BukkitCommandBridge.register(plugin, new SetHomeCommand(plugin, homes));
        BukkitCommandBridge.register(plugin, new DelHomeCommand(plugin, homes));
        BukkitCommandBridge.register(plugin, new HomesCommand(plugin, homes));
        BukkitCommandBridge.register(plugin, new WarpCommand(plugin, warps, state));
        BukkitCommandBridge.register(plugin, new SetWarpCommand(plugin, warps));
        BukkitCommandBridge.register(plugin, new DelWarpCommand(plugin, warps));
        BukkitCommandBridge.register(plugin, new KitCommand(plugin, kits));
        BukkitCommandBridge.register(plugin, new CreateKitCommand(plugin, kits));
        BukkitCommandBridge.register(plugin, new DeleteKitCommand(plugin, kits));
        BukkitCommandBridge.register(plugin, new TpaCommand(plugin, tpa));
        BukkitCommandBridge.register(plugin, new TpaHereCommand(plugin, tpa));
        BukkitCommandBridge.register(plugin, new TpAcceptCommand(plugin, tpa, state));
        BukkitCommandBridge.register(plugin, new TpDenyCommand(plugin, tpa));
        BukkitCommandBridge.register(plugin, new TpCancelCommand(plugin, tpa));
        BukkitCommandBridge.register(plugin, new MsgCommand(plugin, state, ignore));
        BukkitCommandBridge.register(plugin, new ReplyCommand(plugin, state, ignore));
    }
}
