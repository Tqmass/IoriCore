package us.iorilabs.bukkit.menu.api.yaml;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

public final class YamlMenuSession {

    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();

    public Session of(UUID uuid) {
        return sessions.computeIfAbsent(uuid, id -> new Session());
    }

    public void clear(UUID uuid) {
        sessions.remove(uuid);
    }

    public void pushHistory(UUID uuid) {
        Session session = of(uuid);
        if (session.currentId() == null || session.currentId().isEmpty()) return;
        session.history().addLast(new Frame(session.currentId(), copy(session.ctx())));
    }

    public void push(UUID uuid, String menuId, Map<String, String> ctx) {
        pushHistory(uuid);
        Session session = of(uuid);
        session.currentId(menuId);
        session.ctx(copy(ctx));
    }

    public Frame pop(UUID uuid) {
        Session session = of(uuid);
        Frame previous = session.history().pollLast();
        if (previous == null) {
            session.currentId(null);
            session.ctx(Collections.emptyMap());
            return null;
        }
        session.currentId(previous.id());
        session.ctx(copy(previous.ctx()));
        return previous;
    }

    public void page(UUID uuid, String menuId, int page) {
        of(uuid)
            .pages()
            .put(menuId == null ? "" : menuId, Math.max(0, page));
    }

    public int page(UUID uuid, String menuId) {
        Integer value = of(uuid)
            .pages()
            .get(menuId == null ? "" : menuId);
        return value == null ? 0 : Math.max(0, value);
    }

    public void ctx(UUID uuid, Map<String, String> ctx) {
        of(uuid).ctx(copy(ctx));
    }

    public Map<String, String> ctx(UUID uuid) {
        return copy(of(uuid).ctx());
    }

    private static Map<String, String> copy(Map<String, String> source) {
        if (source == null || source.isEmpty()) return new HashMap<>();
        return new HashMap<>(source);
    }

    @Getter
    @Setter
    @Accessors(fluent = true)
    public static final class Session {

        private final Deque<Frame> history = new ArrayDeque<>();
        private final Map<String, Integer> pages = new HashMap<>();
        private String currentId;
        private Map<String, String> ctx = new HashMap<>();
    }

    @Getter
    @Accessors(fluent = true)
    public static final class Frame {

        private final String id;
        private final Map<String, String> ctx;

        public Frame(String id, Map<String, String> ctx) {
            this.id = id;
            this.ctx = ctx == null ? Collections.emptyMap() : ctx;
        }
    }
}
