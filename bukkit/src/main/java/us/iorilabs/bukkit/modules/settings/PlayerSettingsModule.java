package us.iorilabs.bukkit.modules.settings;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import us.iorilabs.api.hook.NLoginHook;
import us.iorilabs.api.hook.NoopNLoginHook;
import us.iorilabs.api.modules.ModuleId;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.commands.BukkitCommandBridge;
import us.iorilabs.bukkit.modules.CoreModule;
import us.iorilabs.bukkit.modules.settings.command.SettingsCommand;

@Getter
@Accessors(fluent = true)
public final class PlayerSettingsModule implements CoreModule {

    private final IoriCorePlugin plugin;
    private final boolean enabledFlag;
    private PlayerSettingsStore store;
    private NLoginHook nLogin = new NoopNLoginHook();
    private Listener quitListener;

    public PlayerSettingsModule(IoriCorePlugin plugin) {
        this.plugin = plugin;
        this.enabledFlag = plugin.settings().getBoolean("MODULES.PLAYER-SETTINGS", true);
    }

    @Override
    public ModuleId id() {
        return ModuleId.PLAYER_SETTINGS;
    }

    @Override
    public boolean enabled() {
        return enabledFlag;
    }

    @Override
    public void enable() {
        store = new PlayerSettingsStore(plugin);
        nLogin = plugin.nLoginHook() == null ? new NoopNLoginHook() : plugin.nLoginHook();

        plugin.language().preferenceResolver(store::cachedLanguage);
        preload(Bukkit.getOnlinePlayers().stream().map(Player::getUniqueId).collect(Collectors.toList()));

        BukkitCommandBridge.register(plugin, new SettingsCommand(plugin, plugin.language()));

        quitListener = new Listener() {
            @EventHandler
            public void onJoin(PlayerJoinEvent event) {
                preload(Collections.singletonList(event.getPlayer().getUniqueId()));
            }

            @EventHandler
            public void onQuit(PlayerQuitEvent event) {
                if (store != null) store.unload(event.getPlayer().getUniqueId());
            }
        };
        Bukkit.getPluginManager().registerEvents(quitListener, plugin);
    }

    @Override
    public void disable() {
        plugin.language().preferenceResolver(uuid -> null);
        if (quitListener != null) {
            HandlerList.unregisterAll(quitListener);
            quitListener = null;
        }
        store = null;
        nLogin = new NoopNLoginHook();
    }

    private void preload(Collection<UUID> uuids) {
        PlayerSettingsStore current = store;
        if (current == null || uuids.isEmpty()) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> uuids.forEach(current::get));
    }
}
