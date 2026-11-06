package us.iorilabs.proxy.vanish;

import com.google.gson.JsonElement;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import us.iorilabs.api.redis.NetworkEventKeys;
import us.iorilabs.proxy.commands.CommandSource;
import us.iorilabs.proxy.player.ProxyPlayer;
import us.iorilabs.proxy.redis.ProxyEventType;
import us.iorilabs.proxy.redis.RedisEvent;
import us.iorilabs.proxy.redis.RedisService;

public final class VanishPresenceService {

    private static final String SEE_PERM = "ioricore.vanish.see";

    private final Set<UUID> vanished = ConcurrentHashMap.newKeySet();

    public VanishPresenceService(RedisService redis) {
        if (redis == null) return;
        redis.subscribe(ProxyEventType.VANISH_UPDATE.id(), this::onVanish);
        redis.subscribe(ProxyEventType.PLAYER_QUIT.id(), this::onQuit);
    }

    public boolean vanished(UUID uuid) {
        return uuid != null && vanished.contains(uuid);
    }

    public boolean visible(CommandSource viewer, ProxyPlayer target) {
        if (target == null) return false;
        if (!vanished(target.uuid())) return true;
        if (viewer == null || viewer.isConsole()) return true;
        return canSeeVanished(viewer);
    }

    public int visibleCount(CommandSource viewer, Iterable<ProxyPlayer> players) {
        int count = 0;
        for (ProxyPlayer player : players) {
            if (visible(viewer, player)) count++;
        }
        return count;
    }

    private void onVanish(RedisEvent event) {
        UUID id = uuid(event);
        Boolean state = state(event);
        if (id == null || state == null) return;
        if (state) vanished.add(id);
        else vanished.remove(id);
    }

    private void onQuit(RedisEvent event) {
        UUID id = uuid(event);
        if (id != null) vanished.remove(id);
    }

    private static UUID uuid(RedisEvent event) {
        if (event == null || event.data() == null || !event.data().has(NetworkEventKeys.UUID)) return null;
        try {
            return UUID.fromString(event.data().get(NetworkEventKeys.UUID).getAsString());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static Boolean state(RedisEvent event) {
        if (event == null || event.data() == null || !event.data().has(NetworkEventKeys.STATE)) return null;
        JsonElement element = event.data().get(NetworkEventKeys.STATE);
        if (element == null || element.isJsonNull()) return null;
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean()) return element.getAsBoolean();
        String raw = element.getAsString();
        if (raw == null || raw.isEmpty()) return null;
        if ("true".equalsIgnoreCase(raw) || "1".equals(raw) || "on".equalsIgnoreCase(raw)) return true;
        if ("false".equalsIgnoreCase(raw) || "0".equals(raw) || "off".equalsIgnoreCase(raw)) return false;
        return null;
    }

    private static boolean canSeeVanished(CommandSource viewer) {
        return (
            viewer.hasPermission(SEE_PERM) ||
            viewer.hasPermission("ioricore.staff") ||
            viewer.hasPermission("ioricore.admin")
        );
    }
}
