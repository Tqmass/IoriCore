package us.iorilabs.bukkit.ranks;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class RankInheritance {

    private final Function<String, Rank> resolver;
    private final String serverName;
    private final Map<UUID, Map<String, Boolean>> cache = new ConcurrentHashMap<>();

    public List<Rank> lineage(Rank rank) {
        if (rank == null) return Collections.emptyList();

        List<Rank> out = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Deque<Rank> pending = new ArrayDeque<>();
        pending.add(rank);
        while (!pending.isEmpty()) {
            Rank current = pending.poll();
            if (!visited.add(current.key())) continue;
            out.add(current);
            for (String parentKey : current.parents()) {
                Rank parent = resolver.apply(parentKey);
                if (parent != null && !visited.contains(parent.key())) pending.add(parent);
            }
        }
        return out;
    }

    public boolean createsCycle(Rank rank, String parentKey) {
        if (rank == null || parentKey == null) return false;

        Deque<String> pending = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        pending.push(parentKey.toLowerCase(Locale.ROOT));
        while (!pending.isEmpty()) {
            String current = pending.pop();
            if (current.equals(rank.key())) return true;
            if (!visited.add(current)) continue;
            Rank node = resolver.apply(current);
            if (node != null) node.parents().forEach(pending::push);
        }
        return false;
    }

    public Map<String, Boolean> permissions(Rank rank) {
        if (rank == null) return Collections.emptyMap();
        return cache.computeIfAbsent(rank.id(), ignored -> Collections.unmodifiableMap(compute(rank)));
    }

    public void invalidate() {
        cache.clear();
    }

    private Map<String, Boolean> compute(Rank rank) {
        Map<String, Boolean> out = new LinkedHashMap<>();
        for (Rank node : lineage(rank)) {
            for (String raw : node.permissions()) merge(out, raw);
        }
        return out;
    }

    // ~ "-perm" niega y "perm@server" solo aplica en ese backend; una negación siempre gana
    private void merge(Map<String, Boolean> out, String raw) {
        if (raw == null) return;
        String node = raw.trim();
        if (node.isEmpty()) return;

        boolean value = !node.startsWith("-");
        if (!value) node = node.substring(1);
        int at = node.lastIndexOf('@');
        if (at > 0) {
            if (!node.substring(at + 1).equalsIgnoreCase(serverName)) return;
            node = node.substring(0, at);
        }
        if (node.isEmpty()) return;
        out.merge(node.toLowerCase(Locale.ROOT), value, (current, next) -> current && next);
    }
}
