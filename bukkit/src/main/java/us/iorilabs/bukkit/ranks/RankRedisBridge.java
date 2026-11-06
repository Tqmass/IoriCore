package us.iorilabs.bukkit.ranks;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import us.iorilabs.api.redis.NetworkEventKeys;
import us.iorilabs.api.redis.NetworkEventType;
import us.iorilabs.bukkit.redis.CoreRedisEvent;
import us.iorilabs.bukkit.redis.CoreRedisService;

@RequiredArgsConstructor
public final class RankRedisBridge {

    private final RankService ranks;
    private final CoreRedisService redis;

    public void register() {
        if (redis == null) return;
        redis.subscribe(NetworkEventType.RANK_UPDATE, this::onRank);
        redis.subscribe(NetworkEventType.GRANT_UPDATE, this::onGrant);
    }

    private void onRank(CoreRedisEvent event) {
        String key = event.string(NetworkEventKeys.RANK_KEY);
        String action = event.string(NetworkEventKeys.ACTION);
        ranks.onRemoteRank(key, action);
    }

    private void onGrant(CoreRedisEvent event) {
        UUID playerId = event.uuid(NetworkEventKeys.UUID);
        if (playerId == null) playerId = event.uuid(NetworkEventKeys.TARGET_UUID);
        ranks.onRemoteGrant(playerId);
    }
}
