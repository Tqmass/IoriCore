package us.iorilabs.bukkit.punishment;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import us.iorilabs.bukkit.IoriCorePlugin;

@Getter
@Accessors(fluent = true)
public final class PunishReasonService {

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
        if (!"punishments.yml".equalsIgnoreCase(pack)) pack = "punishments.yml";

        File rootFile = new File(plugin.getDataFolder(), pack);
        if (!rootFile.exists()) {
            if (plugin.getResource(pack) != null) {
                us.iorilabs.bukkit.util.PluginResources.saveDefault(plugin, pack);
            } else {
                us.iorilabs.bukkit.util.PluginResources.saveDefault(plugin, "punishments.yml");
                rootFile = new File(plugin.getDataFolder(), "punishments.yml");
            }
        }
        return rootFile;
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
