package us.iorilabs.bukkit.menu.api.yaml;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import org.bukkit.entity.Player;

public final class MenuInternalRegistry {

    @FunctionalInterface
    public interface Handler {
        void handle(Player player, String argument, Map<String, String> ctx, YamlMenuService menus);
    }

    @FunctionalInterface
    public interface ContextEnricher {
        void enrich(Player player, String menuId, Map<String, String> ctx);
    }

    private final Map<String, Handler> handlers = new ConcurrentHashMap<>();
    private final Map<String, ContextEnricher> enrichers = new ConcurrentHashMap<>();

    public void register(String id, Handler handler) {
        if (id == null || handler == null) return;
        handlers.put(id.trim().toLowerCase(Locale.ROOT), handler);
    }

    public void registerEnricher(String menuId, ContextEnricher enricher) {
        if (menuId == null || enricher == null) return;
        enrichers.put(menuId.trim().toLowerCase(Locale.ROOT), enricher);
    }

    public void registerEnricherPrefix(String prefix, ContextEnricher enricher) {
        registerEnricher(prefix, enricher);
    }

    public boolean run(String id, Player player, String argument, Map<String, String> ctx, YamlMenuService menus) {
        if (id == null) return false;
        Handler handler = handlers.get(id.trim().toLowerCase(Locale.ROOT));
        if (handler == null) return false;
        handler.handle(player, argument == null ? "" : argument, ctx, menus);
        return true;
    }

    public void enrich(Player player, String menuId, Map<String, String> ctx) {
        if (menuId == null || ctx == null) return;
        String key = menuId.trim().toLowerCase(Locale.ROOT);
        ContextEnricher exact = enrichers.get(key);
        if (exact != null) exact.enrich(player, key, ctx);
        for (Map.Entry<String, ContextEnricher> entry : enrichers.entrySet()) {
            if (entry.getKey().equals(key)) continue;
            if (key.startsWith(entry.getKey())) entry.getValue().enrich(player, key, ctx);
        }
    }

    public void clear() {
        handlers.clear();
        enrichers.clear();
    }

    public void putAll(BiConsumer<String, Handler> consumer) {
        handlers.forEach(consumer);
    }
}
