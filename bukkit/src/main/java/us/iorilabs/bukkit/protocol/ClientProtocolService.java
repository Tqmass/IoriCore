package us.iorilabs.bukkit.protocol;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import us.iorilabs.api.redis.NetworkEventKeys;
import us.iorilabs.api.redis.NetworkEventType;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.redis.CoreRedisEvent;
import us.iorilabs.bukkit.redis.CoreRedisService;
import us.iorilabs.bukkit.util.ProtocolProbe;

public final class ClientProtocolService implements Listener {

    private static final String[] VIA_CLASSES = { "com.viaversion.viaversion.api.Via", "us.myles.ViaVersion.api.Via" };
    private static final long PENDING_TTL_MS = 60_000L;

    private final IoriCorePlugin plugin;
    private final Map<UUID, Integer> known = new ConcurrentHashMap<>();
    private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();
    private final int serverProtocol;

    private volatile Object viaApi;
    private volatile Method viaVersion;

    public ClientProtocolService(IoriCorePlugin plugin) {
        this.plugin = plugin;
        this.serverProtocol = ProtocolProbe.serverFallbackProtocol();
    }

    public void start(CoreRedisService redis) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        Bukkit.getScheduler().runTask(plugin, this::resolveVia);
        if (redis != null) redis.subscribe(NetworkEventType.CLIENT_PROTOCOL, this::onRedis);
    }

    public void stop() {
        HandlerList.unregisterAll(this);
        known.clear();
        pending.clear();
    }

    public int protocol(Player player) {
        if (player == null) return serverProtocol;

        int via = viaProtocol(player.getUniqueId());
        if (via > 0) return via;

        Integer proxy = proxyProtocol(player.getUniqueId());
        if (proxy != null) return proxy;

        return serverProtocol;
    }

    public int serverProtocol() {
        return serverProtocol;
    }

    public void acceptFromConnection(Player player, int protocol) {
        if (player == null || !valid(protocol)) return;
        known.put(player.getUniqueId(), protocol);
        pending.remove(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        known.remove(uuid);
        pending.remove(uuid);
    }

    private Integer proxyProtocol(UUID uuid) {
        Integer value = known.get(uuid);
        if (value != null) return value;

        Pending queued = pending.remove(uuid);
        if (queued == null || System.currentTimeMillis() - queued.receivedAt() > PENDING_TTL_MS) return null;
        known.put(uuid, queued.protocol());
        return queued.protocol();
    }

    private void onRedis(CoreRedisEvent event) {
        UUID uuid = event.uuid(NetworkEventKeys.UUID);
        int protocol = event.integer(NetworkEventKeys.PROTOCOL, -1);
        if (uuid == null || !valid(protocol)) return;

        long now = System.currentTimeMillis();
        pending.values().removeIf(entry -> now - entry.receivedAt() > PENDING_TTL_MS);
        if (known.containsKey(uuid)) {
            known.put(uuid, protocol);
            return;
        }
        pending.put(uuid, new Pending(protocol, now));
    }

    private int viaProtocol(UUID uuid) {
        Object api = viaApi;
        Method method = viaVersion;
        if (api == null || method == null) return -1;
        try {
            Object result = method.invoke(api, uuid);
            return result instanceof Integer value ? value : -1;
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return -1;
        }
    }

    private void resolveVia() {
        for (String className : VIA_CLASSES) {
            try {
                Object api = Class.forName(className).getMethod("getAPI").invoke(null);
                if (api == null) continue;
                Method method = api.getClass().getMethod("getPlayerVersion", UUID.class);
                method.setAccessible(true);
                viaVersion = method;
                viaApi = api;
                return;
            } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {}
        }
    }

    private static boolean valid(int protocol) {
        return protocol > 0;
    }

    private record Pending(int protocol, long receivedAt) {}
}
