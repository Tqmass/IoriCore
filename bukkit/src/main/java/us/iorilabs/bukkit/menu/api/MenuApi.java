package us.iorilabs.bukkit.menu.api;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.scheduler.BukkitTask;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.fx.SoundService;
import us.iorilabs.bukkit.menu.MenuColors;

public final class MenuApi {

    private static final long UPDATE_PERIOD_TICKS = 20L;
    private static final Map<UUID, MenuHolder> SESSIONS = new ConcurrentHashMap<>();

    private static volatile IoriCorePlugin plugin;
    private static ButtonListener listener;
    private static BukkitTask updateTask;

    private MenuApi() {}

    public static synchronized void enable(IoriCorePlugin owner) {
        disable();
        plugin = owner;
        listener = new ButtonListener();
        owner.getServer().getPluginManager().registerEvents(listener, owner);
        updateTask = Bukkit.getScheduler().runTaskTimer(
            owner,
            MenuApi::refreshAutoUpdating,
            UPDATE_PERIOD_TICKS,
            UPDATE_PERIOD_TICKS
        );
    }

    public static synchronized void disable() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
        for (UUID id : SESSIONS.keySet()) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) player.closeInventory();
        }
        SESSIONS.clear();
        if (listener != null) {
            HandlerList.unregisterAll(listener);
            listener = null;
        }
        plugin = null;
    }

    public static boolean enabled() {
        return plugin != null;
    }

    public static SoundService sounds() {
        IoriCorePlugin owner = plugin;
        return owner == null ? null : owner.sounds();
    }

    public static String text(Player player, String key) {
        IoriCorePlugin owner = plugin;
        if (owner == null || owner.language() == null) return MenuColors.legacy(key);
        return owner.language().rawFor(player, key);
    }

    public static MenuHolder session(Player player) {
        return SESSIONS.get(player.getUniqueId());
    }

    public static Menu currentMenu(Player player) {
        MenuHolder holder = session(player);
        return holder == null ? null : holder.menu();
    }

    static void track(Player player, MenuHolder holder) {
        SESSIONS.put(player.getUniqueId(), holder);
    }

    static void untrack(Player player, MenuHolder holder) {
        SESSIONS.remove(player.getUniqueId(), holder);
    }

    static void untrack(UUID id) {
        SESSIONS.remove(id);
    }

    static void sync(Runnable task) {
        IoriCorePlugin owner = plugin;
        if (owner == null) return;
        Bukkit.getScheduler().runTask(owner, task);
    }

    static void warn(String message, Throwable error) {
        IoriCorePlugin owner = plugin;
        if (owner == null) return;
        owner.getLogger().log(Level.WARNING, message, error);
    }

    private static void refreshAutoUpdating() {
        for (Map.Entry<UUID, MenuHolder> entry : SESSIONS.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null) {
                SESSIONS.remove(entry.getKey(), entry.getValue());
                continue;
            }
            Menu menu = entry.getValue().menu();
            if (menu.autoUpdate()) menu.openMenu(player);
        }
    }
}
