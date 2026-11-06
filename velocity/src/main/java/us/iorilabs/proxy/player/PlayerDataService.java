package us.iorilabs.proxy.player;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.experimental.Accessors;
import us.iorilabs.proxy.database.StorageService;
import us.iorilabs.proxy.logging.ProxyLogger;
import us.iorilabs.proxy.platform.ProxyPlatform;

@Getter
@Accessors(fluent = true)
public final class PlayerDataService {

    private final ProxyPlatform platform;
    private final PlayerDataSql sql;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();

    public PlayerDataService(ProxyPlatform platform, StorageService storage, ProxyLogger logger) {
        this.platform = platform;
        this.sql = new PlayerDataSql(storage, logger);
    }

    public void onJoin(UUID uuid, String name, String ip) {
        if (uuid == null || name == null) return;

        long now = System.currentTimeMillis();
        PlayerData data = cache.computeIfAbsent(uuid, id -> new PlayerData(id, name));
        data.touchJoin(name, ip, now);
        platform.asyncExecutor().execute(() -> sql.upsert(data));
    }

    public Optional<PlayerData> get(UUID uuid) {
        if (uuid == null) return Optional.empty();

        PlayerData cached = cache.get(uuid);
        if (cached != null) return Optional.of(cached);

        Optional<PlayerData> loaded = sql.load(uuid);
        loaded.ifPresent(data -> cache.put(uuid, data));
        return loaded;
    }

    public long firstJoinMillis(UUID uuid) {
        return get(uuid).map(PlayerData::firstJoin).orElse(0L);
    }

    public long lastJoinMillis(UUID uuid) {
        return get(uuid).map(PlayerData::lastJoin).orElse(0L);
    }

    public Optional<String> lastIp(UUID uuid) {
        return get(uuid)
            .map(PlayerData::lastIp)
            .filter(ip -> ip != null && !ip.isBlank());
    }

    public void transferAccount(UUID from, UUID to, String toName) {
        PlayerData source = cache.remove(from);
        PlayerData target = cache.computeIfAbsent(to, id ->
            new PlayerData(id, toName == null ? id.toString() : toName)
        );

        if (toName != null && !toName.isBlank()) {
            target.name(toName);
        }

        if (source != null) {
            if (source.firstJoin() > 0) {
                long current = target.firstJoin();
                target.firstJoin(current <= 0 ? source.firstJoin() : Math.min(current, source.firstJoin()));
            }

            if (source.lastJoin() > target.lastJoin()) {
                target.lastJoin(source.lastJoin());
            }

            if (source.lastIp() != null && !source.lastIp().isBlank()) {
                target.lastIp(source.lastIp());
            }
        }

        platform.asyncExecutor().execute(() -> sql.transfer(from, to, target.name()));
    }
}
