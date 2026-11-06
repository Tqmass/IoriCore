package us.iorilabs.bukkit.modules.staffmode;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public final class StaffClickGuard {

    private static final Method INTERACT_HAND = handMethod(PlayerInteractEvent.class);
    private static final Method INTERACT_ENTITY_HAND = handMethod(PlayerInteractEntityEvent.class);

    private final long cooldownMs;
    private final Map<UUID, Long> lastClick = new ConcurrentHashMap<>();

    public StaffClickGuard(long cooldownMs) {
        this.cooldownMs = Math.max(0L, cooldownMs);
    }

    public static boolean mainHand(PlayerInteractEvent event) {
        return mainHand(INTERACT_HAND, event);
    }

    public static boolean mainHand(PlayerInteractEntityEvent event) {
        return mainHand(INTERACT_ENTITY_HAND, event);
    }

    public boolean tryUse(UUID id) {
        if (id == null) return false;
        long now = System.currentTimeMillis();
        Long last = lastClick.get(id);
        if (last != null && now - last < cooldownMs) return false;
        lastClick.put(id, now);
        return true;
    }

    public void forget(UUID id) {
        if (id != null) lastClick.remove(id);
    }

    // 1.8 has no off-hand: getHand() is absent and every interaction is main-hand.
    private static boolean mainHand(Method method, Event event) {
        if (method == null || event == null) return true;
        try {
            Object hand = method.invoke(event);
            return hand == null || "HAND".equals(((Enum<?>) hand).name());
        } catch (ReflectiveOperationException | ClassCastException ignored) {
            return true;
        }
    }

    private static Method handMethod(Class<?> type) {
        try {
            return type.getMethod("getHand");
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }
}
