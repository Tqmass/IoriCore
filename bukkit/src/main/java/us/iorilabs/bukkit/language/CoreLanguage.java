package us.iorilabs.bukkit.language;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import us.iorilabs.bukkit.commands.BukkitCommandSource;
import us.iorilabs.bukkit.storage.StoragePaths;
import us.iorilabs.bukkit.util.ColorUtil;
import us.iorilabs.proxy.commands.CommandSource;

@Accessors(fluent = true)
public final class CoreLanguage {

    private static final String[] LANGS = { "en", "es" };

    @Getter
    private final JavaPlugin plugin;

    private final Map<String, Bundle> bundles = new HashMap<>();

    @Getter
    private String language = "es";

    @Getter
    private int legacyProtocolMax = 340;

    @Getter
    private FileConfiguration formats;

    @Getter
    private FileConfiguration legacyFormats;

    @Getter
    @Setter
    private ProtocolResolver protocolResolver = ProtocolResolver.viaVersionOrServer();

    @Setter
    private Function<UUID, String> preferenceResolver = uuid -> null;

    public CoreLanguage(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load(String languageCode) {
        load(languageCode, 340);
    }

    public void load(String languageCode, int legacyProtocolMax) {
        this.language =
            languageCode == null || languageCode.trim().isEmpty() ? "es" : languageCode.toLowerCase(Locale.ROOT);
        this.legacyProtocolMax = Math.max(0, legacyProtocolMax);
        bundles.clear();

        saveDefaults();
        Set<String> codes = new LinkedHashSet<>();
        Collections.addAll(codes, LANGS);
        codes.add(language);
        for (String code : codes) {
            Bundle bundle = new Bundle();
            flattenFile(modernMessagesPath(code), modernMessagesPath("en"), bundle.modern, bundle.modernLists);
            flattenFile(legacyMessagesPath(code), legacyMessagesPath("en"), bundle.legacy, bundle.legacyLists);
            if (bundle.legacy.isEmpty()) {
                bundle.legacy.putAll(bundle.modern);
                bundle.legacyLists.putAll(bundle.modernLists);
            }
            bundles.put(code, bundle);
        }

        loadFormats();
    }

    public String languageOf(CommandSender sender) {
        if (!(sender instanceof Player)) return language;
        String preferred;
        try {
            preferred = preferenceResolver.apply(((Player) sender).getUniqueId());
        } catch (RuntimeException ex) {
            preferred = null;
        }
        if (preferred == null) return language;
        String code = preferred.trim().toLowerCase(Locale.ROOT);
        return bundles.containsKey(code) ? code : language;
    }

    public int protocolOf(CommandSender sender) {
        if (!(sender instanceof Player)) return Integer.MAX_VALUE;
        return protocolResolver.protocol((Player) sender);
    }

    public boolean legacy(CommandSender sender) {
        return protocolOf(sender) <= legacyProtocolMax;
    }

    public String raw(String key) {
        return raw(key, Collections.emptyMap(), Integer.MAX_VALUE);
    }

    public String raw(String key, Map<String, String> placeholders) {
        return raw(key, placeholders, Integer.MAX_VALUE);
    }

    public String raw(String key, Map<String, String> placeholders, int protocol) {
        return resolve(language, key, placeholders, protocol);
    }

    public String raw(CommandSender sender, String key) {
        return raw(sender, key, Collections.emptyMap());
    }

    public String raw(CommandSender sender, String key, Map<String, String> placeholders) {
        return resolve(languageOf(sender), key, placeholders, protocolOf(sender));
    }

    public String raw(CommandSender sender, String key, String... pairs) {
        return raw(sender, key, pairs(pairs));
    }

    public String raw(CommandSource source, String key) {
        return raw(sender(source), key, Collections.emptyMap());
    }

    public String raw(CommandSource source, String key, Map<String, String> placeholders) {
        return raw(sender(source), key, placeholders);
    }

    public String raw(CommandSource source, String key, String... pairs) {
        return raw(sender(source), key, pairs(pairs));
    }

    public String rawFor(Player player, String key) {
        return raw(player, key, Collections.emptyMap());
    }

    public String rawFor(Player player, String key, Map<String, String> placeholders) {
        return raw(player, key, placeholders);
    }

    public String resolve(String lang, String key, Map<String, String> placeholders, int protocol) {
        Bundle bundle = bundle(lang);
        Map<String, String> bag = bundle.messages(protocol, legacyProtocolMax);
        String normalized = normalize(key);
        String value = bag.get(normalized);
        if (value == null) value = bundle.modern.get(normalized);
        if (value == null) value = bundle(language).modern.get(normalized);
        if (value == null) value = normalized;
        return ColorUtil.translate(substitute(value, placeholders, prefixOf(bag, normalized)), protocol);
    }

    public String plain(String key) {
        return plain(key, Integer.MAX_VALUE);
    }

    public String plain(String key, int protocol) {
        return plain(language, key, protocol);
    }

    public String plain(CommandSender sender, String key) {
        return plain(languageOf(sender), key, protocolOf(sender));
    }

    public String plainLang(String langCode, String key) {
        if (langCode == null || langCode.isBlank()) return plain(language, key, Integer.MAX_VALUE);
        String code = langCode.trim().toLowerCase(Locale.ROOT);
        return plain(code, key, Integer.MAX_VALUE);
    }

    private String plain(String lang, String key, int protocol) {
        Bundle bundle = bundle(lang);
        String normalized = normalize(key);
        String value = bundle.messages(protocol, legacyProtocolMax).get(normalized);
        if (value == null) value = bundle.modern.get(normalized);
        return value == null ? normalized : value;
    }

    public List<String> list(String key) {
        return list(key, Collections.emptyMap(), Integer.MAX_VALUE);
    }

    public List<String> list(String key, Map<String, String> placeholders) {
        return list(key, placeholders, Integer.MAX_VALUE);
    }

    public List<String> list(String key, Map<String, String> placeholders, int protocol) {
        return resolveList(language, key, placeholders, protocol);
    }

    public List<String> list(CommandSender sender, String key) {
        return list(sender, key, Collections.emptyMap());
    }

    public List<String> list(CommandSender sender, String key, Map<String, String> placeholders) {
        return resolveList(languageOf(sender), key, placeholders, protocolOf(sender));
    }

    public List<String> list(CommandSender sender, String key, String... pairs) {
        return list(sender, key, pairs(pairs));
    }

    public List<String> list(CommandSource source, String key) {
        return list(sender(source), key, Collections.emptyMap());
    }

    public List<String> list(CommandSource source, String key, Map<String, String> placeholders) {
        return list(sender(source), key, placeholders);
    }

    public List<String> listFor(Player player, String key) {
        return list(player, key, Collections.emptyMap());
    }

    public List<String> listFor(Player player, String key, Map<String, String> placeholders) {
        return list(player, key, placeholders);
    }

    public List<String> resolveList(String lang, String key, Map<String, String> placeholders, int protocol) {
        Bundle bundle = bundle(lang);
        String normalized = normalize(key);
        List<String> raw = bundle.lists(protocol, legacyProtocolMax).get(normalized);
        if (raw == null || raw.isEmpty()) raw = bundle.modernLists.get(normalized);
        if (raw == null || raw.isEmpty()) raw = bundle(language).modernLists.get(normalized);
        if (raw == null || raw.isEmpty()) return Collections.emptyList();

        Map<String, String> bag = bundle.messages(protocol, legacyProtocolMax);
        String prefix = prefixOf(bag, normalized);
        List<String> out = new ArrayList<>(raw.size());
        for (String line : raw) {
            out.add(ColorUtil.translate(substitute(line, placeholders, prefix), protocol));
        }
        return out;
    }

    public String bool(boolean value) {
        return raw(value ? "RANKS.MENU.BOOL-TRUE" : "RANKS.MENU.BOOL-FALSE");
    }

    public String bool(CommandSender sender, boolean value) {
        return raw(sender, value ? "RANKS.MENU.BOOL-TRUE" : "RANKS.MENU.BOOL-FALSE");
    }

    public String bool(CommandSource source, boolean value) {
        return bool(sender(source), value);
    }

    public String color(String input, CommandSender sender) {
        return ColorUtil.translate(input, protocolOf(sender));
    }

    public String formatTemplate(CommandSender sender, String template, Map<String, String> placeholders) {
        if (template == null || template.isEmpty()) return "";
        String lang = languageOf(sender);
        int protocol = protocolOf(sender);
        Map<String, String> bag = bundle(lang).messages(protocol, legacyProtocolMax);
        Map<String, String> ph = placeholders == null ? Collections.emptyMap() : placeholders;
        return ColorUtil.translate(substitute(template, ph, prefixOf(bag, null)), protocol);
    }

    public FileConfiguration formatsForProtocol(int protocol) {
        return formatsForProtocol(protocol, legacyProtocolMax);
    }

    public FileConfiguration formatsForProtocol(int protocol, int legacyMax) {
        return protocol <= legacyMax ? legacyFormats : formats;
    }

    private Bundle bundle(String lang) {
        Bundle bundle = lang == null ? null : bundles.get(lang);
        if (bundle == null) bundle = bundles.get(language);
        return bundle == null ? Bundle.EMPTY : bundle;
    }

    private static CommandSender sender(CommandSource source) {
        return source instanceof BukkitCommandSource ? ((BukkitCommandSource) source).handle() : null;
    }

    private static Map<String, String> pairs(String... pairs) {
        if (pairs == null || pairs.length < 2) return Collections.emptyMap();
        Map<String, String> out = new HashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            out.put(pairs[i], pairs[i + 1]);
        }
        return out;
    }

    private static String prefixOf(Map<String, String> bag, String key) {
        String global = bag.getOrDefault("PREFIX", "");
        String moduleKey = modulePrefixKey(key);
        if (moduleKey == null) return global;
        String module = bag.getOrDefault(moduleKey, "");
        if (module != null && !module.isBlank()) return module;
        return global;
    }

    private static String modulePrefixKey(String key) {
        if (key == null || key.isEmpty()) return null;
        if (key.startsWith("PUNISHMENTS.")) return "PUNISHMENTS.PREFIX";
        if (key.startsWith("STAFF-MODE.")) return "STAFF-MODE.PREFIX";
        if (key.startsWith("STAFF-AUTH.")) return "STAFF-AUTH.PREFIX";
        return null;
    }

    private static String substitute(String template, Map<String, String> placeholders, String prefix) {
        String value = template
            .replace("<prefix>", prefix)
            .replace("<PREFIX>", prefix)
            .replace("{prefix}", prefix)
            .replace("{PREFIX}", prefix);
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                String token = entry.getValue() == null ? "" : entry.getValue();
                String key = entry.getKey();
                value = value.replace("<" + key + ">", token).replace("{" + key + "}", token);
                value = value.replace("%" + key + "%", token);
                value = value.replace("<" + key.toLowerCase(Locale.ROOT) + ">", token);
                value = value.replace("{" + key.toLowerCase(Locale.ROOT) + "}", token);
                value = value.replace("%" + key.toLowerCase(Locale.ROOT) + "%", token);
            }
        }
        return value;
    }

    private void loadFormats() {
        prepareChatFormatsFile();
        YamlConfiguration root = YamlConfiguration.loadConfiguration(
            new File(plugin.getDataFolder(), "chat-formats.yml")
        );
        if (!root.contains("EN")) {
            root = loadBundledChatFormats();
        }

        String lang = language == null ? "ES" : language.toUpperCase(Locale.ROOT);
        if (layeredChatFormats(root)) {
            formats = mergeChatLayer(root, lang, false);
            legacyFormats = mergeChatLayer(root, lang, true);
            if (legacyFormats == null || legacyFormats.getConfigurationSection("FORMATS") == null) {
                legacyFormats = formats;
            }
            if (formats == null || formats.getConfigurationSection("FORMATS") == null) {
                formats = sectionAsConfig(root, lang, "EN");
                legacyFormats = sectionAsConfig(root, "LEGACY." + lang, "LEGACY.EN");
                if (legacyFormats.getConfigurationSection("FORMATS") == null) {
                    legacyFormats = formats;
                }
            }
            return;
        }

        formats = sectionAsConfig(root, lang, "EN");
        legacyFormats = sectionAsConfig(root, "LEGACY." + lang, "LEGACY.EN");
        if (legacyFormats.getConfigurationSection("FORMATS") == null) {
            legacyFormats = formats;
        }
    }

    private static boolean layeredChatFormats(YamlConfiguration root) {
        ConfigurationSection en = root == null ? null : root.getConfigurationSection("EN");
        return en != null && en.contains("LAYOUT") && en.isConfigurationSection("FORMATS");
    }

    private YamlConfiguration mergeChatLayer(YamlConfiguration root, String lang, boolean legacy) {
        String langKey = lang == null ? "EN" : lang.toUpperCase(Locale.ROOT);
        ConfigurationSection langSection = root.getConfigurationSection(langKey);
        ConfigurationSection legacyRoot = root.getConfigurationSection("LEGACY");
        ConfigurationSection legacyLang = legacyRoot == null ? null : legacyRoot.getConfigurationSection(langKey);
        ConfigurationSection formatsSource = langSection;
        if (legacy && legacyLang != null && legacyLang.isConfigurationSection("FORMATS")) {
            formatsSource = legacyLang;
        }
        if (formatsSource == null || !formatsSource.isConfigurationSection("FORMATS")) {
            return new YamlConfiguration();
        }
        ConfigurationSection formatsIn = formatsSource.getConfigurationSection("FORMATS");
        YamlConfiguration out = new YamlConfiguration();
        ConfigurationSection layoutSource =
            legacy && legacyLang != null && legacyLang.contains("LAYOUT") ? legacyLang : langSection;
        if (layoutSource != null && layoutSource.contains("LAYOUT")) {
            out.set("LAYOUT", layoutSource.getString("LAYOUT"));
        } else if (langSection != null && langSection.contains("LAYOUT")) {
            out.set("LAYOUT", langSection.getString("LAYOUT"));
        }
        ConfigurationSection formatsOut = out.createSection("FORMATS");
        for (String key : formatsIn.getKeys(false)) {
            ConfigurationSection fmt = formatsIn.getConfigurationSection(key);
            if (fmt == null) continue;
            ConfigurationSection fout = formatsOut.createSection(key);
            String perm = fmt.getString("PERMISSION", "");
            if (perm != null && !perm.isEmpty()) fout.set("PERMISSION", perm);
            ConfigurationSection parts = fmt.getConfigurationSection("PARTS");
            fout.set("PARTS", mergeChatParts(parts, fmt));
        }
        return out;
    }

    private static ConfigurationSection mergeChatParts(ConfigurationSection template, ConfigurationSection format) {
        YamlConfiguration out = new YamlConfiguration();
        if (template == null || format == null) return out;
        for (String partKey : template.getKeys(false)) {
            ConfigurationSection p = template.getConfigurationSection(partKey);
            if (p == null) continue;
            ConfigurationSection copy = out.createSection(partKey);
            copy.set("TEXT", colorizeChatPart(p.getString("TEXT", ""), format));
            if (p.isList("HOVER")) copy.set("HOVER", colorizeChatHover(p.getStringList("HOVER"), format));
            if (p.isConfigurationSection("CLICK")) {
                copy.set("CLICK", p.getConfigurationSection("CLICK").getValues(false));
            }
        }
        return out;
    }

    private static String colorizeChatPart(String text, ConfigurationSection format) {
        if (text == null || format == null) return text == null ? "" : text;
        return text
            .replace("{RANK-COLOR}", format.getString("RANK-COLOR", ""))
            .replace("{NAME-COLOR}", format.getString("NAME-COLOR", ""))
            .replace("{MESSAGE-COLOR}", format.getString("MESSAGE-COLOR", ""))
            .replace("{TAG-COLOR}", format.getString("TAG-COLOR", ""));
    }

    private static List<String> colorizeChatHover(List<String> lines, ConfigurationSection format) {
        if (lines == null || lines.isEmpty()) return lines == null ? Collections.emptyList() : lines;
        List<String> out = new ArrayList<>(lines.size());
        for (String line : lines) {
            out.add(colorizeChatPart(line, format));
        }
        return out;
    }

    private void prepareChatFormatsFile() {
        File target = new File(plugin.getDataFolder(), "chat-formats.yml");
        if (target.exists()) return;

        YamlConfiguration merged = new YamlConfiguration();
        boolean any = false;
        for (String lang : LANGS) {
            FileConfiguration modern = readChatFormatFile("chat/formats/" + lang + ".yml");
            if (hasFormats(modern)) {
                attachLangSection(merged, lang.toUpperCase(Locale.ROOT), modern);
                any = true;
            }
        }

        YamlConfiguration legacyRoot = new YamlConfiguration();
        for (String lang : LANGS) {
            FileConfiguration legacy = readChatFormatFile("chat/formats/legacy/" + lang + ".yml");
            if (hasFormats(legacy)) {
                attachLangSection(legacyRoot, lang.toUpperCase(Locale.ROOT), legacy);
                any = true;
            }
        }
        if (!legacyRoot.getKeys(false).isEmpty()) {
            merged.set("LEGACY", legacyRoot);
        }

        if (any) {
            File parent = target.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            try {
                merged.save(target);
            } catch (Exception ignored) {}
            return;
        }

        saveIfMissing("chat-formats.yml");
    }

    private YamlConfiguration loadBundledChatFormats() {
        try (InputStream in = plugin.getResource("chat-formats.yml")) {
            if (in == null) return new YamlConfiguration();
            return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (Exception ignored) {
            return new YamlConfiguration();
        }
    }

    private FileConfiguration readChatFormatFile(String path) {
        File file = new File(plugin.getDataFolder(), path);
        if (file.exists()) return YamlConfiguration.loadConfiguration(file);
        try (InputStream in = plugin.getResource(path)) {
            if (in == null) return new YamlConfiguration();
            return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (Exception ignored) {
            return new YamlConfiguration();
        }
    }

    private static boolean hasFormats(FileConfiguration yaml) {
        return yaml != null && yaml.isConfigurationSection("FORMATS");
    }

    private static void attachLangSection(ConfigurationSection target, String langKey, FileConfiguration source) {
        ConfigurationSection formats = source.getConfigurationSection("FORMATS");
        if (formats == null) return;
        ConfigurationSection langSection = target.createSection(langKey);
        langSection.set("FORMATS", formats.getValues(false));
    }

    private static FileConfiguration sectionAsConfig(YamlConfiguration root, String preferred, String fallback) {
        ConfigurationSection section = root.getConfigurationSection(preferred);
        if (section == null) section = root.getConfigurationSection(fallback);
        YamlConfiguration out = new YamlConfiguration();
        if (section != null) {
            for (String key : section.getKeys(false)) {
                out.set(key, section.get(key));
            }
        }
        return out;
    }

    private void flattenFile(
        String preferred,
        String fallback,
        Map<String, String> messagesOut,
        Map<String, List<String>> listsOut
    ) {
        // Jar defaults first, then disk overrides — missing keys on old installs still resolve.
        flattenResource(preferred, messagesOut, listsOut);
        File file = new File(plugin.getDataFolder(), preferred);
        if (!file.exists()) {
            file = new File(plugin.getDataFolder(), fallback);
            flattenResource(fallback, messagesOut, listsOut);
        }
        if (!file.exists()) return;
        flatten(YamlConfiguration.loadConfiguration(file), "", messagesOut, listsOut);
    }

    private void flattenResource(String path, Map<String, String> messagesOut, Map<String, List<String>> listsOut) {
        try (InputStream in = plugin.getResource(path)) {
            if (in == null) return;
            flatten(
                YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8)),
                "",
                messagesOut,
                listsOut
            );
        } catch (Exception ignored) {}
    }

    private void saveDefaults() {
        migrateMessageFiles();
        for (String lang : LANGS) {
            saveIfMissing(modernMessagesPath(lang));
            saveIfMissing(legacyMessagesPath(lang));
        }
        saveIfMissing("chat-formats.yml");
    }

    private void migrateMessageFiles() {
        File data = plugin.getDataFolder();
        for (String lang : LANGS) {
            StoragePaths.migrateIfPresent(
                new File(data, "messages-" + lang + ".yml"),
                new File(data, modernMessagesPath(lang))
            );
            StoragePaths.migrateIfPresent(
                new File(data, "messages-" + lang + "-legacy.yml"),
                new File(data, legacyMessagesPath(lang))
            );
            StoragePaths.migrateIfPresent(
                new File(data, "messages/messages-" + lang + ".yml"),
                new File(data, modernMessagesPath(lang))
            );
            StoragePaths.migrateIfPresent(
                new File(data, "messages/messages-" + lang + "-legacy.yml"),
                new File(data, legacyMessagesPath(lang))
            );
        }
    }

    private static String modernMessagesPath(String code) {
        return "messages/" + code + ".yml";
    }

    private static String legacyMessagesPath(String code) {
        return "messages/legacy/" + code + ".yml";
    }

    private void saveIfMissing(String path) {
        File file = new File(plugin.getDataFolder(), path);
        if (file.exists()) return;
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        plugin.saveResource(path, false);
    }

    private void flatten(
        ConfigurationSection section,
        String path,
        Map<String, String> messagesOut,
        Map<String, List<String>> listsOut
    ) {
        for (String key : section.getKeys(false)) {
            String next = path.isEmpty() ? key : path + "." + key;
            if (section.isConfigurationSection(key)) {
                flatten(section.getConfigurationSection(key), next, messagesOut, listsOut);
                continue;
            }
            if (section.isList(key)) {
                List<String> values = section.getStringList(key);
                listsOut.put(normalize(next), values == null ? Collections.emptyList() : values);
                continue;
            }
            Object value = section.get(key);
            if (value != null) messagesOut.put(normalize(next), String.valueOf(value));
        }
    }

    private static String normalize(String key) {
        return key == null ? "" : key.trim().toUpperCase(Locale.ROOT).replace('_', '-').replace(' ', '-');
    }

    private static final class Bundle {

        private static final Bundle EMPTY = new Bundle();

        private final Map<String, String> modern = new HashMap<>();
        private final Map<String, List<String>> modernLists = new HashMap<>();
        private final Map<String, String> legacy = new HashMap<>();
        private final Map<String, List<String>> legacyLists = new HashMap<>();

        private Map<String, String> messages(int protocol, int legacyMax) {
            return protocol <= legacyMax ? legacy : modern;
        }

        private Map<String, List<String>> lists(int protocol, int legacyMax) {
            return protocol <= legacyMax ? legacyLists : modernLists;
        }
    }
}
