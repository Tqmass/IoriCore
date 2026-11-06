package us.iorilabs.bukkit.ranks;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.RequiredArgsConstructor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import us.iorilabs.bukkit.ranks.enums.RankFlag;
import us.iorilabs.bukkit.ranks.enums.RankScope;
import us.iorilabs.bukkit.ranks.enums.RankType;

@RequiredArgsConstructor
public final class RankYaml {

    private static final String FILE = "ranks.yml";
    private static final String[] LEGACY_KEYS = {
        "NAME",
        "DISPLAY-NAME",
        "PREFIX",
        "SUFFIX",
        "PRIORITY",
        "COLOR",
        "ITALIC",
        "BOLD",
        "STAFF",
        "MEDIA",
        "ADMIN",
        "OP",
        "VISIBLE",
        "DEFAULT",
        "PERMISSION-COUNT",
    };

    private final JavaPlugin plugin;
    private final Logger logger;
    private final Object lock = new Object();

    public List<Rank> load() {
        File file = file();
        if (!file.exists()) plugin.saveResource(FILE, false);

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("RANKS");
        if (root == null) {
            logger.warning("ranks.yml missing RANKS section");
            return Collections.emptyList();
        }

        ConfigurationSection defaults = yaml.getConfigurationSection("DEFAULTS");
        String defaultKey = yaml.getString("DEFAULT-RANK", "");
        List<Rank> ranks = new ArrayList<>();
        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) continue;
            Rank rank = isLegacy(section) ? parseLegacy(key, section) : parse(key, section, defaults);
            if (!defaultKey.isEmpty()) rank.defaultRank(key.equalsIgnoreCase(defaultKey));
            ranks.add(rank);
        }
        return ranks;
    }

    public void save(Rank rank) {
        if (rank == null || rank.key() == null) return;

        synchronized (lock) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file());
            ConfigurationSection root = section(yaml, "RANKS");
            ConfigurationSection node = root.getConfigurationSection(rank.key());
            if (node == null) node = root.createSection(rank.key());

            for (String legacy : LEGACY_KEYS) node.set(legacy, null);
            node.set("ID", rank.id().toString());
            if (rank.name() != null && !rank.name().equals(rank.key())) node.set("NAME", rank.name());
            node.set("WEIGHT", rank.weight());
            node.set("TYPE", rank.type().name());
            node.set("SCOPE", rank.scope().name());
            node.set("FLAGS", flagNames(rank.flags()));
            node.set("PARENTS", new ArrayList<>(rank.parents()));
            node.set("VISUAL.DISPLAY", nullToEmpty(rank.displayName()));
            node.set("VISUAL.PREFIX", nullToEmpty(rank.prefix()));
            node.set("VISUAL.SUFFIX", nullToEmpty(rank.suffix()));
            node.set("VISUAL.COLOR", nullToEmpty(rank.colorKey()));
            node.set("VISUAL.BOLD", rank.bold());
            node.set("VISUAL.ITALIC", rank.italic());
            node.set("PERMISSIONS", new ArrayList<>(rank.permissions()));
            if (rank.defaultRank()) yaml.set("DEFAULT-RANK", rank.key());
            write(yaml);
        }
    }

    public void delete(String key) {
        if (key == null) return;

        synchronized (lock) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file());
            ConfigurationSection root = section(yaml, "RANKS");
            root.set(key, null);
            write(yaml);
        }
    }

    private Rank parse(String key, ConfigurationSection section, ConfigurationSection defaults) {
        Rank rank = base(key, section);
        rank.name(section.getString("NAME", key))
            .weight(section.getInt("WEIGHT", 0))
            .type(RankType.from(fallback(section, defaults, "TYPE", "NORMAL")))
            .scope(RankScope.from(fallback(section, defaults, "SCOPE", "GLOBAL")))
            .displayName(fallback(section, defaults, "VISUAL.DISPLAY", key))
            .prefix(fallback(section, defaults, "VISUAL.PREFIX", ""))
            .suffix(fallback(section, defaults, "VISUAL.SUFFIX", ""))
            .colorKey(fallback(section, defaults, "VISUAL.COLOR", "GRAY"))
            .bold(bool(section, defaults, "VISUAL.BOLD"))
            .italic(bool(section, defaults, "VISUAL.ITALIC"))
            .defaultRank(section.getBoolean("DEFAULT", false));

        List<String> flags = section.isList("FLAGS")
            ? section.getStringList("FLAGS")
            : defaults == null
              ? Collections.emptyList()
              : defaults.getStringList("FLAGS");
        rank.flags(parseFlags(flags));
        return rank;
    }

    private Rank parseLegacy(String key, ConfigurationSection section) {
        Rank rank = base(key, section);
        rank.name(section.getString("NAME", key))
            .displayName(section.getString("DISPLAY-NAME", key))
            .prefix(section.getString("PREFIX", ""))
            .suffix(section.getString("SUFFIX", ""))
            .weight(section.getInt("PRIORITY", section.getInt("WEIGHT", 0)))
            .type(RankType.from(section.getString("TYPE", "NORMAL")))
            .scope(RankScope.from(section.getString("SCOPE", "GLOBAL")))
            .colorKey(section.getString("COLOR", "GRAY"))
            .italic(section.getBoolean("ITALIC", false))
            .bold(section.getBoolean("BOLD", false))
            .defaultRank(section.getBoolean("DEFAULT", false));

        Set<RankFlag> flags = parseFlags(section.getStringList("FLAGS"));
        if (section.getBoolean("STAFF", false)) flags.add(RankFlag.STAFF);
        if (section.getBoolean("ADMIN", false)) flags.add(RankFlag.ADMIN);
        if (section.getBoolean("MEDIA", false)) flags.add(RankFlag.MEDIA);
        if (!section.getBoolean("VISIBLE", true)) flags.add(RankFlag.HIDDEN);
        rank.flags(flags);
        return rank;
    }

    private static Rank base(String key, ConfigurationSection section) {
        String normalized = key.toLowerCase(Locale.ROOT);
        Rank rank = new Rank();
        rank.key(normalized).id(parseId(section.getString("ID", ""), normalized));
        rank.parents(lowerAll(section.getStringList("PARENTS")));
        rank.permissions(new ArrayList<>(section.getStringList("PERMISSIONS")));
        return rank;
    }

    private static boolean isLegacy(ConfigurationSection section) {
        return section.contains("PRIORITY") || section.contains("DISPLAY-NAME") || section.isString("PREFIX");
    }

    private static String fallback(
        ConfigurationSection section,
        ConfigurationSection defaults,
        String path,
        String def
    ) {
        if (section.contains(path)) return section.getString(path, def);
        if (defaults != null && defaults.contains(path)) return defaults.getString(path, def);
        return def;
    }

    private static boolean bool(ConfigurationSection section, ConfigurationSection defaults, String path) {
        if (section.contains(path)) return section.getBoolean(path, false);
        return defaults != null && defaults.getBoolean(path, false);
    }

    private static Set<RankFlag> parseFlags(List<String> raw) {
        Set<RankFlag> flags = EnumSet.noneOf(RankFlag.class);
        if (raw == null) return flags;
        for (String entry : raw) {
            RankFlag flag = RankFlag.from(entry);
            if (flag != null) flags.add(flag);
        }
        return flags;
    }

    private static List<String> flagNames(Set<RankFlag> flags) {
        List<String> out = new ArrayList<>();
        for (RankFlag flag : flags) out.add(flag.name());
        return out;
    }

    private static List<String> lowerAll(List<String> values) {
        List<String> out = new ArrayList<>();
        if (values == null) return out;
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) out.add(value.trim().toLowerCase(Locale.ROOT));
        }
        return out;
    }

    private static UUID parseId(String raw, String key) {
        if (raw == null || raw.trim().isEmpty()) return Rank.idFor(key);
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException ex) {
            return Rank.idFor(key);
        }
    }

    private static ConfigurationSection section(YamlConfiguration yaml, String path) {
        ConfigurationSection section = yaml.getConfigurationSection(path);
        return section == null ? yaml.createSection(path) : section;
    }

    private void write(YamlConfiguration yaml) {
        try {
            yaml.save(file());
        } catch (IOException ex) {
            logger.log(Level.WARNING, "ranks.yml save failed", ex);
        }
    }

    private File file() {
        return new File(plugin.getDataFolder(), FILE);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
