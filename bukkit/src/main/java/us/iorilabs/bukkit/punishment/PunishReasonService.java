package us.iorilabs.bukkit.punishment;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import us.iorilabs.bukkit.IoriCorePlugin;

@Getter
@Accessors(fluent = true)
public final class PunishReasonService {

    private static final int CATALOG_VERSION = 1;

    private final IoriCorePlugin plugin;
    private final PunishmentConfig config;
    private final List<PunishReason> reasons = new ArrayList<>();

    public PunishReasonService(IoriCorePlugin plugin, PunishmentConfig config) {
        this.plugin = plugin;
        this.config = config;
        reload();
    }

    public void reload() {
        reasons.clear();
        File file = resolveCatalogFile();
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String typeKey : yaml.getKeys(false)) {
            if ("CONFIG-VERSION".equalsIgnoreCase(typeKey)) continue;
            List<?> entries = yaml.getList(typeKey);
            if (entries == null) continue;
            for (Object entry : entries) {
                if (entry instanceof java.util.Map<?, ?> map) {
                    PunishReason reason = fromMap(typeKey, map);
                    if (reason != null) reasons.add(reason);
                }
            }
        }
    }

    private File resolveCatalogFile() {
        String pack =
            config == null || config.reasonsPackFile() == null || config.reasonsPackFile().isBlank()
                ? "punishments.yml"
                : config.reasonsPackFile().trim();
        if ("punish-reasons.yml".equalsIgnoreCase(pack)) pack = "punishments.yml";

        File rootFile = new File(plugin.getDataFolder(), pack);
        File dataLegacy = new File(plugin.getDataFolder(), "data/punish-reasons.yml");
        us.iorilabs.bukkit.storage.StoragePaths.migrateIfPresent(dataLegacy, rootFile);

        if (!rootFile.exists()) {
            if (plugin.getResource(pack) != null) {
                us.iorilabs.bukkit.util.PluginResources.saveDefault(plugin, pack);
            } else {
                us.iorilabs.bukkit.util.PluginResources.saveDefault(plugin, "punishments.yml");
                rootFile = new File(plugin.getDataFolder(), "punishments.yml");
            }
        }

        ensureCatalogVersion(rootFile, plugin.getLogger());
        return rootFile;
    }

    private static void ensureCatalogVersion(File rootFile, Logger logger) {
        if (rootFile == null || !rootFile.exists()) return;

        FileConfiguration yaml = YamlConfiguration.loadConfiguration(rootFile);
        if (yaml.contains("CONFIG-VERSION") && yaml.getInt("CONFIG-VERSION", 0) >= CATALOG_VERSION) return;

        String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.ROOT).format(new Date());
        File backup = new File(rootFile.getParentFile(), rootFile.getName() + ".bak-" + stamp);
        try {
            Files.copy(rootFile.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ex) {
            if (logger != null) {
                logger.log(Level.WARNING, "Punish reasons · backup failed · " + rootFile.getName(), ex);
            }
            return;
        }

        yaml.set("CONFIG-VERSION", CATALOG_VERSION);
        try {
            yaml.save(rootFile);
            if (logger != null) {
                logger.info("Punish reasons · CONFIG-VERSION=" + CATALOG_VERSION + " · " + rootFile.getName());
            }
        } catch (Exception ex) {
            if (logger != null) {
                logger.log(Level.WARNING, "Punish reasons · migrate save failed · " + rootFile.getName(), ex);
            }
        }
    }

    public List<PunishReason> byType(String type) {
        if (type == null || type.isBlank()) return Collections.unmodifiableList(reasons);
        String wanted = type.trim().toUpperCase(Locale.ROOT);
        List<PunishReason> out = new ArrayList<>();
        for (PunishReason reason : reasons) {
            if (wanted.equals(reason.type())) out.add(reason);
        }
        return out;
    }

    public PunishReason byId(String id) {
        if (id == null || id.isBlank()) return null;
        String wanted = id.trim().toLowerCase(Locale.ROOT);
        for (PunishReason reason : reasons) {
            if (wanted.equals(reason.id().toLowerCase(Locale.ROOT))) return reason;
        }
        return null;
    }

    private PunishReason fromMap(String typeKey, java.util.Map<?, ?> map) {
        Object idRaw = map.get("ID");
        Object commandRaw = map.get("COMMAND");
        if (idRaw == null || commandRaw == null) return null;

        Object loreRaw = map.get("LORE");
        List<String> lore = new ArrayList<>();
        if (loreRaw instanceof List<?> list) {
            for (Object line : list) {
                if (line != null) lore.add(String.valueOf(line));
            }
        }

        Object reasonRaw = map.get("REASON");
        String executor = text(map.get("EXECUTOR"), "CONSOLE").trim().toUpperCase(Locale.ROOT);
        if (!"PLAYER".equals(executor)) executor = "CONSOLE";

        return new PunishReason(
            typeKey,
            String.valueOf(idRaw),
            text(map.get("MATERIAL"), "PAPER"),
            text(map.get("MATERIAL-FALLBACK"), "PAPER"),
            text(map.get("NAME"), String.valueOf(idRaw)),
            lore,
            reasonRaw == null ? "" : String.valueOf(reasonRaw),
            text(map.get("DURATION"), ""),
            String.valueOf(commandRaw),
            executor
        );
    }

    private static String text(Object value, String fallback) {
        if (value == null) return fallback;
        String raw = String.valueOf(value);
        return raw.isEmpty() ? fallback : raw;
    }

    @Getter
    @RequiredArgsConstructor
    @Accessors(fluent = true)
    public static final class PunishReason {

        private final String type;
        private final String id;
        private final String material;
        private final String materialFallback;
        private final String name;
        private final List<String> lore;
        private final String reason;
        private final String duration;
        private final String command;
        private final String executor;
    }
}
