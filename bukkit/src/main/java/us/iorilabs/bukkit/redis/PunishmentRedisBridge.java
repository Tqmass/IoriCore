package us.iorilabs.bukkit.redis;

import com.google.gson.JsonObject;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import us.iorilabs.api.punishment.PunishmentType;
import us.iorilabs.api.redis.NetworkEventKeys;
import us.iorilabs.api.redis.NetworkEventType;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.modules.CoreModule;
import us.iorilabs.bukkit.punishment.Punishment;
import us.iorilabs.bukkit.punishment.PunishmentService;
import us.iorilabs.bukkit.punishment.PunishmentsModule;

@RequiredArgsConstructor
public final class PunishmentRedisBridge {

    private final IoriCorePlugin plugin;
    private final CoreRedisService redis;

    public void register() {
        redis.subscribe(NetworkEventType.PUNISHMENT_APPLIED, this::onRemote);
        redis.subscribe(NetworkEventType.PUNISHMENT_INVALIDATE, this::onRemote);
    }

    public void applied(Punishment punishment) {
        if (punishment == null || punishment.targetId() == null) return;

        JsonObject data = new JsonObject();
        data.addProperty(NetworkEventKeys.TYPE, punishment.type().name());
        data.addProperty(NetworkEventKeys.TARGET_UUID, punishment.targetId().toString());
        data.addProperty(NetworkEventKeys.TARGET_NAME, punishment.targetName() == null ? "" : punishment.targetName());
        data.addProperty(NetworkEventKeys.ACTOR, punishment.actorName() == null ? "" : punishment.actorName());
        data.addProperty(NetworkEventKeys.REASON, punishment.reason() == null ? "" : punishment.reason());
        data.addProperty(NetworkEventKeys.EXPIRES_AT, punishment.expiresAt());
        redis.publish(NetworkEventType.PUNISHMENT_APPLIED, data);
    }

    public void invalidated(UUID targetId, PunishmentType type) {
        if (targetId == null) return;

        JsonObject data = new JsonObject();
        data.addProperty(NetworkEventKeys.UUID, targetId.toString());
        if (type != null) data.addProperty(NetworkEventKeys.TYPE, type.name());
        redis.publish(NetworkEventType.PUNISHMENT_INVALIDATE, data);
    }

    private void onRemote(CoreRedisEvent event) {
        UUID target = event.uuid(NetworkEventKeys.TARGET_UUID);
        if (target == null) target = event.uuid(NetworkEventKeys.UUID);
        if (target == null) return;

        PunishmentService service = service();
        if (service == null) return;

        if (event.data() != null &&
            event.data().has(NetworkEventKeys.PERSIST) &&
            event.data().get(NetworkEventKeys.PERSIST).getAsBoolean()) {
            persistFromProxy(service, event);
        }

        service.invalidateBanCache(target);
        service.invalidateMuteCache(target);
        UUID refresh = target;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (Bukkit.getPlayer(refresh) == null) return;
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> service.refreshMute(refresh));
        });
    }

    private void persistFromProxy(PunishmentService service, CoreRedisEvent event) {
        JsonObject data = event.data();
        if (data == null || !data.has(NetworkEventKeys.TYPE)) return;

        PunishmentType type;
        try {
            type = PunishmentType.valueOf(data.get(NetworkEventKeys.TYPE).getAsString().trim().toUpperCase());
        } catch (Exception ex) {
            return;
        }

        UUID target = event.uuid(NetworkEventKeys.TARGET_UUID);
        if (target == null) return;

        String targetName = data.has(NetworkEventKeys.TARGET_NAME)
            ? data.get(NetworkEventKeys.TARGET_NAME).getAsString()
            : "";
        String actor = data.has(NetworkEventKeys.ACTOR) ? data.get(NetworkEventKeys.ACTOR).getAsString() : "Discord";
        String reason = data.has(NetworkEventKeys.REASON) ? data.get(NetworkEventKeys.REASON).getAsString() : "";
        long expiresAt = data.has(NetworkEventKeys.EXPIRES_AT) ? data.get(NetworkEventKeys.EXPIRES_AT).getAsLong() : 0L;

        service.punishFromNetwork(type, target, targetName, actor, reason, expiresAt);
    }

    private PunishmentService service() {
        if (plugin.modules() == null) return null;
        for (CoreModule module : plugin.modules().all()) {
            if (module instanceof PunishmentsModule punishments) return punishments.punishments();
        }
        return null;
    }
}
