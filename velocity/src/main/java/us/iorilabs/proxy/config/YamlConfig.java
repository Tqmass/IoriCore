package us.iorilabs.proxy.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import us.iorilabs.proxy.logging.ProxyLogger;
import us.iorilabs.proxy.platform.ProxyPlatform;

@Accessors(fluent = true)
public final class YamlConfig {

    private final ProxyPlatform platform;
    private final ProxyLogger logger;

    @Getter
    private final String fileName;

    @Getter
    private final Path path;

    @Getter
    private Map<String, Object> root = new LinkedHashMap<>();

    private final YamlConfig parent;
    private final String sectionPrefix;

    public YamlConfig(ProxyPlatform platform, ProxyLogger logger, String fileName) {
        this.platform = platform;
        this.logger = logger;
        this.fileName = fileName;
        this.path = platform.dataDirectory().resolve(fileName);
        this.parent = null;
        this.sectionPrefix = null;
    }

    private YamlConfig(YamlConfig parent, String sectionPrefix) {
        this.platform = parent.platform;
        this.logger = parent.logger;
        this.fileName = parent.fileName;
        this.path = parent.path;
        this.root = parent.root;
        this.parent = parent;
        this.sectionPrefix = sectionPrefix;
    }

    public YamlConfig section(String prefix) {
        if (prefix == null || prefix.isBlank()) return this;

        YamlConfig base = parent == null ? this : parent;
        String combined = sectionPrefix == null ? prefix : sectionPrefix + "." + prefix;
        return new YamlConfig(base, combined);
    }

    public void load() {
        try {
            Files.createDirectories(path.getParent());

            if (!Files.exists(path)) {
                try (InputStream in = platform.getResource(fileName)) {
                    if (in != null) {
                        Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
                    } else {
                        Files.createFile(path);
                    }
                }
            }

            Yaml yaml = new Yaml();

            try (InputStream in = Files.newInputStream(path)) {
                Object loaded = yaml.load(in);
                if (loaded instanceof Map<?, ?> map) {
                    root = normalizeTree(castMap(map));
                } else {
                    root = new LinkedHashMap<>();
                }
            }
        } catch (IOException ex) {
            logger.error("Failed to load config file: " + fileName, ex);
            root = new LinkedHashMap<>();
        }
    }

    public void save() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);
        Yaml yaml = new Yaml(options);

        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            yaml.dump(root, writer);
        } catch (IOException ex) {
            logger.error("Failed to save config file: " + fileName, ex);
        }
    }

    public void saveTopLevelList(String key, List<String> values) {
        String normalized = normalizeKey(key);
        root.put(normalized, new ArrayList<>(values));

        try {
            List<String> lines = Files.exists(path)
                ? new ArrayList<>(Files.readAllLines(path, StandardCharsets.UTF_8))
                : new ArrayList<>();
            int start = -1;

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.startsWith(" ") || line.startsWith("#") || !line.contains(":")) continue;

                if (normalizeKey(line.substring(0, line.indexOf(':')).trim()).equals(normalized)) {
                    start = i;
                    break;
                }
            }

            List<String> block = new ArrayList<>();

            if (values.isEmpty()) {
                block.add(normalized + ": []");
            } else {
                block.add(normalized + ":");

                for (String value : values) {
                    block.add("  - \"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"");
                }
            }

            if (start < 0) {
                lines.addAll(block);
            } else {
                int end = start + 1;

                while (end < lines.size() && lines.get(end).stripLeading().startsWith("- ")) {
                    end++;
                }

                lines.subList(start, end).clear();
                lines.addAll(start, block);
            }

            Files.write(path, lines, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            logger.error("Failed to save config file: " + fileName, ex);
        }
    }

    public boolean contains(String pathKey) {
        return resolve(pathKey) != null;
    }

    public String getString(String pathKey, String def) {
        Object value = resolve(pathKey);
        return value == null ? def : String.valueOf(value);
    }

    public int getInt(String pathKey, int def) {
        Object value = resolve(pathKey);
        if (value instanceof Number number) return number.intValue();

        if (value instanceof String text) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                return def;
            }
        }

        return def;
    }

    public double getDouble(String pathKey, double def) {
        Object value = resolve(pathKey);
        if (value instanceof Number number) return number.doubleValue();

        if (value instanceof String text) {
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException ignored) {
                return def;
            }
        }

        return def;
    }

    public boolean getBoolean(String pathKey, boolean def) {
        Object value = resolve(pathKey);
        if (value instanceof Boolean bool) return bool;
        if (value instanceof String text) return Boolean.parseBoolean(text);

        return def;
    }

    @SuppressWarnings("unchecked")
    public List<String> getStringList(String pathKey) {
        Object value = resolve(pathKey);
        if (value instanceof List<?> list) return list.stream().map(String::valueOf).toList();

        return Collections.emptyList();
    }

    public List<Object> getList(String pathKey) {
        Object value = resolve(pathKey);
        if (value instanceof List<?> list) return new ArrayList<>(list);

        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getSection(String pathKey) {
        Object value = resolve(pathKey);
        if (value instanceof Map<?, ?> map) return castMap(map);

        return Collections.emptyMap();
    }

    private Object resolve(String pathKey) {
        if (sectionPrefix != null && !sectionPrefix.isBlank()) {
            pathKey = sectionPrefix + "." + pathKey;
        }

        Map<String, Object> rootMap = parent == null ? root : parent.root;
        String[] parts = pathKey.split("\\.");
        Object current = rootMap;

        for (String part : parts) {
            if (!(current instanceof Map<?, ?> map)) return null;

            String normalized = normalizeKey(part);
            Object next = map.get(normalized);
            if (next == null) {
                next = map.get(part);
            }
            if (next == null) return null;
            current = next;
        }

        return current;
    }

    public static String normalizeKey(String key) {
        if (key == null || key.isBlank()) return "";
        return key.replace('_', '-').toUpperCase(Locale.ROOT);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> normalizeTree(Map<String, Object> source) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = normalizeKey(String.valueOf(entry.getKey()));
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> map) {
                out.put(key, normalizeTree(castMap(map)));
            } else if (value instanceof List<?> list) {
                List<Object> copy = new ArrayList<>(list.size());
                for (Object item : list) {
                    if (item instanceof Map<?, ?> nested) {
                        copy.add(normalizeTree(castMap(nested)));
                    } else {
                        copy.add(item);
                    }
                }
                out.put(key, copy);
            } else {
                out.put(key, value);
            }
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Map<?, ?> map) {
        Map<String, Object> casted = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            casted.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return casted;
    }
}
