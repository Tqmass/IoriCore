package us.iorilabs.bukkit.menu.api.yaml;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.menu.enums.MenuVariant;
import us.iorilabs.bukkit.util.ProtocolProbe;
import us.iorilabs.proxy.utilities.ProtocolVersions;

@Getter
@Accessors(fluent = true)
public final class YamlMenuService {

    private final IoriCorePlugin plugin;
    private final MenuInternalRegistry internals = new MenuInternalRegistry();
    private final MenuDynamicRegistry dynamics = new MenuDynamicRegistry();
    private final YamlMenuSession sessions = new YamlMenuSession();
    private final YamlItemFactory items;
    private final YamlActionRunner runner;
    private final Map<String, YamlMenuDefinition> cache = new ConcurrentHashMap<>();
    private MenuStyleSettings menuStyle;

    public YamlMenuService(IoriCorePlugin plugin) {
        this.plugin = plugin;
        this.items = new YamlItemFactory(plugin);
        this.runner = new YamlActionRunner(this);
        this.menuStyle = new MenuStyleSettings(plugin);
    }

    public void reload() {
        cache.clear();
        this.menuStyle = new MenuStyleSettings(plugin);
        loadVariant(MenuVariant.MODERN);
        loadVariant(MenuVariant.LEGACY);
    }

    public int size() {
        return cache.size();
    }

    public MenuVariant variant(Player player) {
        int protocol =
            player == null
                ? ProtocolProbe.serverFallbackProtocol()
                : ProtocolProbe.protocol(player, plugin.fallbackProtocol());
        return ProtocolVersions.isLegacy(protocol) ? MenuVariant.LEGACY : MenuVariant.MODERN;
    }

    public YamlMenuDefinition definition(Player player, String id) {
        if (id == null || id.trim().isEmpty()) return null;
        return definition(variant(player), id);
    }

    public YamlMenuDefinition definition(MenuVariant variant, String id) {
        if (id == null || id.trim().isEmpty()) return null;
        String key = cacheKey(variant, id);
        YamlMenuDefinition cached = cache.get(key);
        if (cached != null) return cached;
        YamlMenuDefinition loaded = loadOne(variant, normalize(id));
        if (loaded != null) cache.put(key, loaded);
        return loaded;
    }

    public void open(Player player, String id) {
        open(player, id, Collections.emptyMap(), true);
    }

    public void open(Player player, String id, Map<String, String> ctx) {
        open(player, id, ctx, true);
    }

    public void open(Player player, String id, Map<String, String> ctx, boolean pushHistory) {
        if (player == null || !player.isOnline()) return;
        String menuId = normalize(id);
        YamlMenuDefinition definition = definition(player, menuId);
        if (definition == null || !definition.inventoryMenu()) {
            plugin.getLogger().warning("Unknown yaml menu: " + menuId);
            return;
        }

        Map<String, String> merged = new HashMap<>();
        if (ctx != null) merged.putAll(ctx);
        internals.enrich(player, menuId, merged);
        UUID uuid = player.getUniqueId();
        if (pushHistory) sessions.pushHistory(uuid);
        sessions.of(uuid).currentId(menuId);
        sessions.ctx(uuid, merged);

        new YamlMenu(this, definition, sessions.ctx(player.getUniqueId()), items, runner).openMenu(player);
    }

    public void refresh(Player player) {
        if (player == null) return;
        String id = sessions.of(player.getUniqueId()).currentId();
        if (id == null) return;
        open(player, id, sessions.ctx(player.getUniqueId()), false);
    }

    public void back(Player player) {
        if (player == null) return;
        YamlMenuSession.Frame previous = sessions.pop(player.getUniqueId());
        if (previous == null) {
            player.closeInventory();
            return;
        }
        open(player, previous.id(), previous.ctx(), false);
    }

    public void navigateBack(Player player) {
        if (player == null) return;
        YamlMenuSession.Session session = sessions.of(player.getUniqueId());
        if (!session.history().isEmpty()) {
            back(player);
            return;
        }
        String currentId = session.currentId();
        YamlMenuDefinition current = currentId == null ? null : definition(player, currentId);
        if (current != null && current.parentId() != null && !current.parentId().isEmpty()) {
            open(player, current.parentId(), sessions.ctx(player.getUniqueId()), false);
            return;
        }
        player.closeInventory();
    }

    public void clearSession(Player player) {
        if (player != null) sessions.clear(player.getUniqueId());
    }

    public int menuSeparatorWidth() {
        return menuStyle == null ? 26 : menuStyle.separatorWidth();
    }

    public String themeSoundFor(String themeName, String key) {
        if (menuStyle == null || themeName == null || themeName.isEmpty()) return "";
        YamlMenuTheme theme = menuStyle.theme(themeName);
        if (theme == null) return "";
        return theme.soundForItem(key);
    }

    private void loadVariant(MenuVariant variant) {
        File folder = new File(plugin.getDataFolder(), "menus/" + variant.folder());
        if (!folder.exists()) folder.mkdirs();
        saveBundled(variant);
        List<File> files = new ArrayList<>();
        collectMenuFiles(folder, files);
        Map<String, List<String>> idPaths = new HashMap<>();
        for (File file : files) {
            String rel = relativeMenuPath(folder, file);
            String id = normalizeIdFromRelative(rel);
            idPaths.computeIfAbsent(cacheKey(variant, id), k -> new ArrayList<>()).add(rel);
        }
        for (Map.Entry<String, List<String>> e : idPaths.entrySet()) {
            if (e.getValue().size() > 1) {
                plugin.getLogger().severe("Duplicate menu id " + e.getKey() + " · " + e.getValue());
            }
        }
        for (File file : files) {
            String rel = relativeMenuPath(folder, file);
            String id = normalizeIdFromRelative(rel);
            try {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
                YamlMenuDefinition def = YamlMenuDefinition.from(yaml, variant, id);
                if (def.id() == null || def.id().isEmpty()) def.id(id);
                applyTheme(def);
                cache.put(cacheKey(variant, def.id()), def);
            } catch (RuntimeException ex) {
                plugin.getLogger().log(Level.WARNING, "Failed loading menu " + rel, ex);
            }
        }
    }

    private static void collectMenuFiles(File dir, List<File> out) {
        File[] list = dir.listFiles();
        if (list == null) return;
        for (File f : list) {
            if (f.isDirectory()) {
                collectMenuFiles(f, out);
            } else if (f.getName().toLowerCase(Locale.ROOT).endsWith(".yml")) {
                out.add(f);
            }
        }
    }

    private static String relativeMenuPath(File root, File file) {
        String rootPath = root.getAbsolutePath();
        String filePath = file.getAbsolutePath();
        if (!filePath.startsWith(rootPath)) return file.getName();
        String rest = filePath.substring(rootPath.length());
        if (rest.startsWith(File.separator)) rest = rest.substring(1);
        return rest.replace('\\', '/');
    }

    private static String normalizeIdFromRelative(String relative) {
        if (relative == null) return "";
        String base = relative;
        int slash = base.lastIndexOf('/');
        if (slash >= 0) base = base.substring(slash + 1);
        if (base.toLowerCase(Locale.ROOT).endsWith(".yml")) {
            base = base.substring(0, base.length() - 4);
        }
        return normalize(base);
    }

    private void applyTheme(YamlMenuDefinition def) {
        if (menuStyle == null || def == null) return;
        String themeKey = def.themeName();
        if (themeKey == null || themeKey.isEmpty()) return;
        YamlMenuTheme theme = menuStyle.theme(themeKey);
        if (theme == null) return;
        theme.apply(def);
    }

    private YamlMenuDefinition loadOne(MenuVariant variant, String id) {
        String[] candidates = {
            "menus/" + variant.folder() + "/" + id + ".yml",
            "menus/" + variant.folder() + "/submenus/" + id + ".yml",
        };
        YamlConfiguration yaml = null;
        for (String resource : candidates) {
            File file = new File(plugin.getDataFolder(), resource);
            us.iorilabs.bukkit.util.PluginResources.saveDefault(plugin, resource);
            if (file.exists()) {
                yaml = YamlConfiguration.loadConfiguration(file);
                break;
            }
            try (InputStream in = plugin.getResource(resource)) {
                if (in != null) {
                    yaml = YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
                    break;
                }
            } catch (Exception ignored) {}
        }
        if (yaml == null) return null;
        YamlMenuDefinition def = YamlMenuDefinition.from(yaml, variant, id);
        applyTheme(def);
        return def;
    }

    private void saveBundled(MenuVariant variant) {
        String[] ids = {
            "settings",
            "settings-sounds",
            "settings-particles",
            "settings-lang",
            "rank-list",
            "rank-edit",
            "rank-parents",
            "coinshop",
            "coinshop-tags",
            "coinshop-confirm",
            "staff-mode",
            "kits",
            "staff-inspect",
            "staff-players",
            "punish",
            "submenus/punish-reasons",
            "tags",
            "glow",
        };
        for (String id : ids) {
            us.iorilabs.bukkit.util.PluginResources.saveDefault(
                plugin,
                "menus/" + variant.folder() + "/" + id + ".yml"
            );
        }
    }

    private static String cacheKey(MenuVariant variant, String id) {
        return variant.folder() + "|" + normalize(id);
    }

    private static String normalize(String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
