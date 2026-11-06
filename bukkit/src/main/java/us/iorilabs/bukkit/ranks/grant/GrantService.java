package us.iorilabs.bukkit.ranks.grant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.RequiredArgsConstructor;
import us.iorilabs.bukkit.ranks.Rank;
import us.iorilabs.bukkit.ranks.enums.RankScope;

@RequiredArgsConstructor
public final class GrantService {

    private final GrantSql sql;
    private final String serverName;
    private final Map<UUID, List<Grant>> cache = new ConcurrentHashMap<>();

    public boolean available() {
        return sql.available();
    }

    public void load(UUID playerId) {
        if (playerId == null) return;
        cache.put(playerId, new CopyOnWriteArrayList<>(sql.active(playerId, System.currentTimeMillis())));
    }

    public void unload(UUID playerId) {
        if (playerId != null) cache.remove(playerId);
    }

    public void cacheClear() {
        cache.clear();
    }

    public boolean loaded(UUID playerId) {
        return playerId != null && cache.containsKey(playerId);
    }

    public List<Grant> active(UUID playerId) {
        List<Grant> cached = playerId == null ? null : cache.get(playerId);
        if (cached == null || cached.isEmpty()) return Collections.emptyList();

        long now = System.currentTimeMillis();
        List<Grant> out = new ArrayList<>();
        for (Grant grant : cached) {
            if (grant.active(now, serverName)) out.add(grant);
        }
        return out;
    }

    public Grant add(UUID playerId, Rank rank, long durationMillis, String addedBy, String reason) {
        long now = System.currentTimeMillis();
        Grant grant = new Grant();
        grant
            .playerId(playerId)
            .rankId(rank.id())
            .scope(rank.scope())
            .serverKey(rank.scope() == RankScope.GLOBAL ? "" : serverName)
            .addedBy(addedBy)
            .reason(reason)
            .createdAt(now)
            .expiresAt(durationMillis > 0L ? now + durationMillis : 0L);
        if (!sql.insert(grant)) return null;

        List<Grant> cached = cache.get(playerId);
        if (cached != null) cached.add(grant);
        return grant;
    }

    public int remove(UUID playerId, Rank rank, String removedBy, String reason) {
        int removed = sql.remove(playerId, rank.id(), removedBy, reason, System.currentTimeMillis());
        List<Grant> cached = cache.get(playerId);
        if (cached != null) cached.removeIf(grant -> rank.id().equals(grant.rankId()));
        return removed;
    }

    public void dropRank(UUID rankId) {
        for (List<Grant> grants : cache.values()) grants.removeIf(grant -> rankId.equals(grant.rankId()));
    }

    public List<Grant> history(UUID playerId, int limit) {
        return sql.history(playerId, limit);
    }

    public Set<UUID> sweepExpired() {
        long now = System.currentTimeMillis();
        Set<UUID> changed = new HashSet<>();
        for (Map.Entry<UUID, List<Grant>> entry : cache.entrySet()) {
            if (entry.getValue().removeIf(grant -> grant.expired(now))) changed.add(entry.getKey());
        }
        return changed;
    }
}
