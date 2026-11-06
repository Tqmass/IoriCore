package us.iorilabs.proxy.language;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.kyori.adventure.text.Component;
import us.iorilabs.proxy.config.YamlConfig;
import us.iorilabs.proxy.logging.ProxyLogger;
import us.iorilabs.proxy.platform.ProxyPlatform;
import us.iorilabs.proxy.player.ProxyPlayer;
import us.iorilabs.proxy.utilities.ColorUtil;
import us.iorilabs.proxy.utilities.TextCenter;

@Accessors(fluent = true)
public final class LanguageManager {

    private final ProxyPlatform platform;
    private final ProxyLogger logger;
    private final Map<String, String> modernMessages = new HashMap<>();
    private final Map<String, List<String>> modernLists = new HashMap<>();
    private final Map<String, String> legacyMessages = new HashMap<>();
    private final Map<String, List<String>> legacyLists = new HashMap<>();

    @Getter
    private String language = "es";

    @Getter
    private int legacyProtocolMax = 340;

    public LanguageManager(ProxyPlatform platform, ProxyLogger logger) {
        this.platform = platform;
        this.logger = logger;
    }

    public void load(String languageCode) {
        load(languageCode, 340, "");
    }

    public void load(String languageCode, int legacyProtocolMax) {
        load(languageCode, legacyProtocolMax, "");
    }

    public void load(String languageCode, int legacyProtocolMax, String defaultPrefix) {
        this.language = languageCode == null || languageCode.isBlank() ? "es" : languageCode.toLowerCase(Locale.ROOT);
        this.legacyProtocolMax = Math.max(0, legacyProtocolMax);
        modernMessages.clear();
        modernLists.clear();
        legacyMessages.clear();
        legacyLists.clear();

        migrateMessageFiles();
        loadFile(modernMessagesPath(language), modernMessages, modernLists);
        loadFile(legacyMessagesPath(language), legacyMessages, legacyLists);

        if (modernMessages.isEmpty() && !"en".equals(language)) {
            logger.warn("Language '" + language + "' empty, falling back to en");
            load("en", this.legacyProtocolMax, defaultPrefix);
            return;
        }

        if (legacyMessages.isEmpty()) {
            legacyMessages.putAll(modernMessages);
            legacyLists.putAll(modernLists);
            logger.warn("No legacy messages for '" + language + "', using modern as fallback");
        }

        applyDefaultPrefix(defaultPrefix);

        logger.module(
            "Language",
            "Loaded modern=" +
                modernMessages.size() +
                " legacy=" +
                legacyMessages.size() +
                " (" +
                language +
                ", legacy<=p" +
                this.legacyProtocolMax +
                ")"
        );
    }

    public String raw(String key) {
        return raw(key, null, Integer.MAX_VALUE);
    }

    public String raw(String key, Map<String, String> placeholders) {
        return raw(key, placeholders, Integer.MAX_VALUE);
    }

    public String raw(String key, Map<String, String> placeholders, int protocol) {
        Map<String, String> bag = protocol <= legacyProtocolMax ? legacyMessages : modernMessages;
        String normalized = normalizeKey(key);
        String value = bag.getOrDefault(normalized, modernMessages.getOrDefault(normalized, key));
        return substitute(value, placeholders, prefixOf(bag));
    }

    public String format(String template, Map<String, String> placeholders) {
        return substitute(template, placeholders, prefixOf(modernMessages));
    }

    private String prefixOf(Map<String, String> bag) {
        return bag.getOrDefault("PREFIX", modernMessages.getOrDefault("PREFIX", ""));
    }

    private static String substitute(String template, Map<String, String> placeholders, String prefix) {
        String value = template.replace("<prefix>", prefix).replace("<PREFIX>", prefix);

        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                String name = entry.getKey();
                String replacement = entry.getValue() == null ? "" : entry.getValue();
                value = value.replace("<" + name + ">", replacement);
                value = value.replace("{" + name + "}", replacement);
                value = value.replace("%" + name + "%", replacement);
            }
        }

        return TextCenter.apply(value);
    }

    public String rawFor(ProxyPlayer player, String key) {
        return raw(key, null, player == null ? Integer.MAX_VALUE : player.protocolVersion());
    }

    public String rawFor(ProxyPlayer player, String key, Map<String, String> placeholders) {
        return raw(key, placeholders, player == null ? Integer.MAX_VALUE : player.protocolVersion());
    }

    public Component component(String key) {
        return ColorUtil.parse(raw(key));
    }

    public Component component(String key, Map<String, String> placeholders) {
        return ColorUtil.parse(raw(key, placeholders));
    }

    public List<String> rawList(String key) {
        return rawList(key, null, Integer.MAX_VALUE);
    }

    public List<String> rawList(String key, Map<String, String> placeholders) {
        return rawList(key, placeholders, Integer.MAX_VALUE);
    }

    public List<String> rawList(String key, int protocol) {
        return rawList(key, null, protocol);
    }

    public List<String> rawList(String key, Map<String, String> placeholders, int protocol) {
        String normalized = normalizeKey(key);
        Map<String, List<String>> lists = protocol <= legacyProtocolMax ? legacyLists : modernLists;
        List<String> stored = lists.get(normalized);
        if (stored == null || stored.isEmpty()) {
            stored = modernLists.get(normalized);
        }

        String prefix = prefixOf(protocol <= legacyProtocolMax ? legacyMessages : modernMessages);
        if (stored != null && !stored.isEmpty()) {
            return stored
                .stream()
                .map(line -> substitute(line, placeholders, prefix))
                .toList();
        }

        String flat = raw(key, placeholders, protocol);
        if (flat == null || flat.isBlank() || flat.equals(normalized) || flat.equals(key)) return List.of();

        return List.of(flat.split("\n", -1));
    }

    public Set<String> listChildKeys(String parentKey, int protocol) {
        String parent = normalizeKey(parentKey);
        Map<String, List<String>> lists = protocol <= legacyProtocolMax ? legacyLists : modernLists;
        Set<String> children = new LinkedHashSet<>();
        String prefix = parent + ".";

        for (String key : lists.keySet()) {
            if (!key.startsWith(prefix)) continue;

            String rest = key.substring(prefix.length());
            int dot = rest.indexOf('.');
            children.add(dot < 0 ? rest : rest.substring(0, dot));
        }

        if (children.isEmpty() && protocol <= legacyProtocolMax) {
            for (String key : modernLists.keySet()) {
                if (!key.startsWith(prefix)) continue;

                String rest = key.substring(prefix.length());
                int dot = rest.indexOf('.');
                children.add(dot < 0 ? rest : rest.substring(0, dot));
            }
        }

        return children;
    }

    public static String normalizeKey(String key) {
        if (key == null || key.isBlank()) return "";

        String[] parts = key.split("\\.");
        StringBuilder out = new StringBuilder();

        for (String part : parts) {
            if (out.length() > 0) {
                out.append('.');
            }

            out.append(part.replace('_', '-').toUpperCase(Locale.ROOT));
        }

        return out.toString();
    }

    private void applyDefaultPrefix(String defaultPrefix) {
        if (defaultPrefix == null || defaultPrefix.isBlank()) return;

        if (modernMessages.getOrDefault("PREFIX", "").isBlank()) {
            modernMessages.put("PREFIX", defaultPrefix);
        }

        if (legacyMessages.getOrDefault("PREFIX", "").isBlank()) {
            legacyMessages.put("PREFIX", ColorUtil.toLegacySection(defaultPrefix));
        }
    }

    private void loadFile(String path, Map<String, String> messagesOut, Map<String, List<String>> listsOut) {
        YamlConfig file = new YamlConfig(platform, logger, path);
        file.load();
        flatten("", file.root(), messagesOut, listsOut);
    }

    private void migrateMessageFiles() {
        Path data = platform.dataDirectory();
        for (String lang : List.of("en", "es")) {
            migrateFile(data.resolve("messages-" + lang + ".yml"), data.resolve(modernMessagesPath(lang)));
            migrateFile(data.resolve("messages-" + lang + "-legacy.yml"), data.resolve(legacyMessagesPath(lang)));
        }
    }

    private static void migrateFile(Path oldPath, Path newPath) {
        try {
            if (!Files.exists(oldPath) || Files.exists(newPath)) return;
            Path parent = newPath.getParent();
            if (parent != null) Files.createDirectories(parent);
            Files.move(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {}
    }

    private static String modernMessagesPath(String code) {
        return "messages/" + code + ".yml";
    }

    private static String legacyMessagesPath(String code) {
        return "messages/legacy/" + code + ".yml";
    }

    @SuppressWarnings("unchecked")
    private void flatten(
        String prefix,
        Map<String, Object> source,
        Map<String, String> out,
        Map<String, List<String>> listOut
    ) {
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String segment = entry.getKey().replace('_', '-').toUpperCase(Locale.ROOT);
            String key = prefix.isEmpty() ? segment : prefix + "." + segment;
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> map) {
                Map<String, Object> nested = new HashMap<>();

                for (Map.Entry<?, ?> nestedEntry : map.entrySet()) {
                    nested.put(String.valueOf(nestedEntry.getKey()), nestedEntry.getValue());
                }

                flatten(key, nested, out, listOut);
            } else if (value instanceof Iterable<?> iterable) {
                List<String> lines = new ArrayList<>();

                for (Object line : iterable) {
                    lines.add(String.valueOf(line));
                }

                listOut.put(key, Collections.unmodifiableList(lines));
                out.put(key, String.join("\n", lines));
            } else if (value != null) {
                out.put(key, String.valueOf(value));
            }
        }
    }
}
