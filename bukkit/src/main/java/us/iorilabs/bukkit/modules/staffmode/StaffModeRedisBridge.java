package us.iorilabs.bukkit.modules.staffmode;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import us.iorilabs.api.modules.ModuleId;
import us.iorilabs.api.redis.NetworkEventKeys;
import us.iorilabs.api.redis.NetworkEventType;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.modules.core.CoreFeaturesModule;
import us.iorilabs.bukkit.modules.core.vanish.VanishService;
import us.iorilabs.bukkit.modules.staffmode.freeze.FreezeService;
import us.iorilabs.bukkit.redis.CoreRedisEvent;
import us.iorilabs.bukkit.redis.CoreRedisService;

@RequiredArgsConstructor
public final class StaffModeRedisBridge {

    private final IoriCorePlugin plugin;
    private final CoreRedisService redis;

    public void register() {
        if (redis == null) return;
        redis.subscribe(NetworkEventType.FREEZE_UPDATE, this::onFreeze);
        redis.subscribe(NetworkEventType.VANISH_UPDATE, this::onVanish);
    }

    public boolean syncEnabled() {
        if (redis == null || !redis.enabled()) return false;
        StaffModeModule module = plugin.modules() == null ? null : plugin.modules().get(ModuleId.STAFF_MODE);
        if (module != null && module.service() != null) {
            return module.service().config().getBoolean("REDIS-SYNC", true);
        }
        return StaffModeConfig.load(plugin).getBoolean("REDIS-SYNC", true);
    }

    public void publishFreeze(UUID uuid, boolean frozen) {
        publish(NetworkEventType.FREEZE_UPDATE, uuid, frozen);
    }

    public void publishVanish(UUID uuid, boolean vanished) {
        publish(NetworkEventType.VANISH_UPDATE, uuid, vanished);
    }

    private void publish(NetworkEventType type, UUID uuid, boolean state) {
        if (!syncEnabled() || uuid == null) return;

        JsonObject data = new JsonObject();
        data.addProperty(NetworkEventKeys.UUID, uuid.toString());
        data.addProperty(NetworkEventKeys.STATE, state);
        data.addProperty(NetworkEventKeys.SERVER, plugin.serverName());
        redis.publish(type, data);
    }

    private void onFreeze(CoreRedisEvent event) {
        if (!syncEnabled()) return;
        UUID id = event.uuid(NetworkEventKeys.UUID);
        Boolean state = state(event);
        if (id == null || state == null) return;

        Bukkit.getScheduler().runTask(plugin, () -> {
            FreezeService freeze = freeze();
            if (freeze != null) freeze.applyRemote(id, state);
        });
    }

    private void onVanish(CoreRedisEvent event) {
        if (!syncEnabled()) return;
        UUID id = event.uuid(NetworkEventKeys.UUID);
        Boolean state = state(event);
        if (id == null || state == null) return;

        Bukkit.getScheduler().runTask(plugin, () -> {
            VanishService vanish = vanish();
            if (vanish != null) vanish.applyRemote(id, state);
        });
    }

    private FreezeService freeze() {
        if (plugin.modules() == null) return null;
        StaffModeModule module = plugin.modules().get(ModuleId.STAFF_MODE);
        if (module == null || !module.enabled() || module.service() == null) return null;
        return module.service().freeze();
    }

    private VanishService vanish() {
        if (plugin.modules() == null) return null;
        CoreFeaturesModule essentials = plugin.modules().get(ModuleId.CORE);
        if (essentials != null && essentials.enabled() && essentials.vanish() != null) return essentials.vanish();
        StaffModeModule module = plugin.modules().get(ModuleId.STAFF_MODE);
        if (module == null || module.service() == null) return null;
        return module.service().vanish();
    }

    private static Boolean state(CoreRedisEvent event) {
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
}
