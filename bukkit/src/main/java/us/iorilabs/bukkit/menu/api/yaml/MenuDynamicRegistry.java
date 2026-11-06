package us.iorilabs.bukkit.menu.api.yaml;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;

public final class MenuDynamicRegistry {

    @FunctionalInterface
    public interface Provider {
        List<Map<String, String>> provide(Player player, Map<String, String> ctx);
    }

    private final Map<String, Provider> providers = new ConcurrentHashMap<>();

    public void register(String source, Provider provider) {
        if (source == null || provider == null) return;
        providers.put(source.trim().toLowerCase(Locale.ROOT), provider);
    }

    public List<Map<String, String>> provide(String source, Player player, Map<String, String> ctx) {
        if (source == null || source.trim().isEmpty()) return Collections.emptyList();
        Provider provider = providers.get(source.trim().toLowerCase(Locale.ROOT));
        if (provider == null) return Collections.emptyList();
        List<Map<String, String>> entries = provider.provide(player, ctx);
        return entries == null ? Collections.emptyList() : entries;
    }

    public void clear() {
        providers.clear();
    }
}
