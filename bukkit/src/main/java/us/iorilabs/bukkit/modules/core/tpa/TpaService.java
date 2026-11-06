package us.iorilabs.bukkit.modules.core.tpa;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.config.CoreFeatureSettings;

@Getter
@Accessors(fluent = true)
public final class TpaService {

    private final IoriCorePlugin plugin;
    private final Map<UUID, TpaRequest> incoming = new ConcurrentHashMap<>();

    public TpaService(IoriCorePlugin plugin) {
        this.plugin = plugin;
    }

    public TpaRequest request(Player requester, Player target, boolean here) {
        if (requester == null || target == null) return null;
        long timeout = Math.max(5L, CoreFeatureSettings.getLong(plugin.settings(), "TPA-TIMEOUT-SECONDS", 60L));
        TpaRequest request = new TpaRequest(
            requester.getUniqueId(),
            target.getUniqueId(),
            here,
            System.currentTimeMillis() + timeout * 1000L
        );
        incoming.put(target.getUniqueId(), request);
        return request;
    }

    public TpaRequest peek(UUID target) {
        if (target == null) return null;
        TpaRequest request = incoming.get(target);
        if (request == null) return null;
        if (request.expired(System.currentTimeMillis())) {
            incoming.remove(target, request);
            return null;
        }
        return request;
    }

    public TpaRequest take(UUID target) {
        TpaRequest request = peek(target);
        if (request == null) return null;
        incoming.remove(target, request);
        return request;
    }

    public void clear(UUID playerId) {
        if (playerId == null) return;
        incoming.remove(playerId);
        incoming.entrySet().removeIf(entry -> playerId.equals(entry.getValue().requester()));
    }

    public TpaRequest cancelOutgoing(UUID requesterId) {
        if (requesterId == null) return null;
        for (Map.Entry<UUID, TpaRequest> entry : incoming.entrySet()) {
            TpaRequest request = entry.getValue();
            if (request == null) continue;
            if (!requesterId.equals(request.requester())) continue;
            if (request.expired(System.currentTimeMillis())) {
                incoming.remove(entry.getKey(), request);
                continue;
            }
            if (incoming.remove(entry.getKey(), request)) return request;
        }
        return null;
    }

    public Player requesterOnline(TpaRequest request) {
        if (request == null) return null;
        return Bukkit.getPlayer(request.requester());
    }
}
