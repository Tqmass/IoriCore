package us.iorilabs.bukkit.util;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.modules.core.vanish.VanishVisibility;

public final class DisplayYaml {

    private static final Pattern ANIMATION_TOKEN = Pattern.compile("<animation:([^>]+)>", Pattern.CASE_INSENSITIVE);
    private static final Pattern TIME_TOKEN = Pattern.compile("<time(?::([^>]+))?>", Pattern.CASE_INSENSITIVE);
    private static final Pattern SERVER_TOKEN = Pattern.compile(
        "<server_([^>_]+)(?:_(online|max|version|status))?>",
        Pattern.CASE_INSENSITIVE
    );

    private DisplayYaml() {}

    public static FileConfiguration load(JavaPlugin plugin, String fileName) {
        PluginResources.saveDefault(plugin, fileName);
        return YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), fileName));
    }

    public static String sectionKey(boolean legacy) {
        return legacy ? "LEGACY" : "MODERN";
    }

    public static int frameIndex(long serverTicks, int intervalTicks, int frameCount) {
        if (frameCount <= 0) return 0;
        long step = Math.max(1L, intervalTicks);
        return (int) ((serverTicks / step) % frameCount);
    }

    public static String resolveTitle(
        FileConfiguration config,
        String sectionKey,
        String style,
        long serverTicks,
        Map<String, String> placeholders,
        int protocol,
        IoriCorePlugin plugin
    ) {
        ConfigurationSection styleSection = styleSection(config, sectionKey, style);
        YamlAnimation animation = readAnimation(config, sectionKey, styleSection, "TITLE-ANIMATED");
        if (animation.enabled() && !animation.stringFrames().isEmpty()) {
            int index = frameIndex(serverTicks, animation.intervalTicks(), animation.stringFrames().size());
            return color(animation.stringFrames().get(index), config, serverTicks, placeholders, protocol, plugin);
        }
        String styled = styleSection == null ? null : styleSection.getString("TITLE");
        if (styled != null && !styled.isEmpty()) {
            return color(styled, config, serverTicks, placeholders, protocol, plugin);
        }
        return color(config.getString(sectionKey + ".TITLE", ""), config, serverTicks, placeholders, protocol, plugin);
    }

    public static List<String> resolveScoreboardLines(
        FileConfiguration config,
        String sectionKey,
        String style,
        long serverTicks,
        Map<String, String> placeholders,
        int protocol,
        IoriCorePlugin plugin
    ) {
        List<String> lines = new ArrayList<>();
        appendLines(lines, config, sectionKey + ".BARS", null, serverTicks, placeholders, protocol, plugin);
        ConfigurationSection styleSection = styleSection(config, sectionKey, style);
        if (styleSection != null && styleSection.isList("LINES")) {
            appendLines(
                lines,
                styleSection.getStringList("LINES"),
                config,
                serverTicks,
                placeholders,
                protocol,
                plugin
            );
        } else {
            appendLines(lines, config, sectionKey + ".LINES", null, serverTicks, placeholders, protocol, plugin);
        }
        YamlAnimation footerAnim = readAnimation(config, sectionKey, styleSection, "FOOTER-ANIMATED");
        if (footerAnim.enabled() && !footerAnim.listFrames().isEmpty()) {
            int index = frameIndex(serverTicks, footerAnim.intervalTicks(), footerAnim.listFrames().size());
            appendLines(footerAnim.listFrames().get(index), config, serverTicks, placeholders, protocol, plugin, lines);
        } else if (styleSection != null && styleSection.isList("FOOTER")) {
            appendLines(
                lines,
                styleSection.getStringList("FOOTER"),
                config,
                serverTicks,
                placeholders,
                protocol,
                plugin
            );
        } else {
            appendLines(lines, config, sectionKey + ".FOOTER", null, serverTicks, placeholders, protocol, plugin);
        }
        return lines;
    }

    public static List<String> resolveLines(
        FileConfiguration config,
        String sectionKey,
        String listPath,
        Map<String, String> placeholders,
        int protocol,
        IoriCorePlugin plugin
    ) {
        List<String> lines = new ArrayList<>();
        appendLines(lines, config, sectionKey + "." + listPath, null, 0L, placeholders, protocol, plugin);
        return lines;
    }

    public static boolean hasGlobalAnimations(FileConfiguration config) {
        ConfigurationSection section = config == null ? null : config.getConfigurationSection("ANIMATIONS");
        return section != null && !section.getKeys(false).isEmpty();
    }

    private static ConfigurationSection styleSection(FileConfiguration config, String sectionKey, String style) {
        if (style != null && !style.isEmpty()) {
            ConfigurationSection styles = config.getConfigurationSection("STYLES");
            ConfigurationSection styled = findStylesChild(styles, style);
            if (styled != null) {
                ConfigurationSection variant = styled.getConfigurationSection(sectionKey);
                if (variant != null) return variant;
            }
            ConfigurationSection nested = config.getConfigurationSection(sectionKey + ".STYLES." + style);
            if (nested != null) return nested;
        }
        return config.getConfigurationSection(sectionKey);
    }

    private static ConfigurationSection findStylesChild(ConfigurationSection styles, String style) {
        if (styles == null || style == null || style.isEmpty()) return null;
        ConfigurationSection direct = styles.getConfigurationSection(style);
        if (direct != null) return direct;
        String normalized = style.toLowerCase(Locale.ROOT);
        for (String key : styles.getKeys(false)) {
            if (key.equalsIgnoreCase(style) || key.toLowerCase(Locale.ROOT).equals(normalized)) {
                return styles.getConfigurationSection(key);
            }
        }
        return null;
    }

    private static ConfigurationSection findAnimationSection(FileConfiguration config, String id) {
        if (config == null || id == null || id.isEmpty()) return null;
        ConfigurationSection root = config.getConfigurationSection("ANIMATIONS");
        if (root == null) return null;
        ConfigurationSection direct = root.getConfigurationSection(id);
        if (direct != null) return direct;
        for (String key : root.getKeys(false)) {
            if (key.equalsIgnoreCase(id)) return root.getConfigurationSection(key);
        }
        return null;
    }

    private static String resolveAnimationTokens(
        String template,
        FileConfiguration config,
        long serverTicks,
        Map<String, String> placeholders,
        int protocol,
        IoriCorePlugin plugin
    ) {
        if (template == null || config == null || !template.contains("<animation:")) return template;
        Matcher matcher = ANIMATION_TOKEN.matcher(template);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String id = matcher.group(1).trim();
            String frame = globalAnimationFrame(config, id, serverTicks, placeholders, protocol, plugin);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(frame));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String globalAnimationFrame(
        FileConfiguration config,
        String id,
        long serverTicks,
        Map<String, String> placeholders,
        int protocol,
        IoriCorePlugin plugin
    ) {
        YamlAnimation animation = YamlAnimation.from(findAnimationSection(config, id));
        if (!animation.enabled() || animation.stringFrames().isEmpty()) return "";
        int index = frameIndex(serverTicks, animation.intervalTicks(), animation.stringFrames().size());
        return color(animation.stringFrames().get(index), null, 0L, placeholders, protocol, plugin);
    }

    private static YamlAnimation readAnimation(
        FileConfiguration config,
        String sectionKey,
        ConfigurationSection styleSection,
        String key
    ) {
        if (styleSection != null && styleSection.isConfigurationSection(key)) {
            return YamlAnimation.from(styleSection.getConfigurationSection(key));
        }
        ConfigurationSection section = config.getConfigurationSection(sectionKey + "." + key);
        return YamlAnimation.from(section);
    }

    private static void appendLines(
        List<String> target,
        FileConfiguration config,
        String path,
        FileConfiguration animationConfig,
        long serverTicks,
        Map<String, String> placeholders,
        int protocol,
        IoriCorePlugin plugin
    ) {
        if (!config.isList(path)) return;
        appendLines(target, config.getStringList(path), animationConfig, serverTicks, placeholders, protocol, plugin);
    }

    private static void appendLines(
        List<String> target,
        List<String> source,
        FileConfiguration animationConfig,
        long serverTicks,
        Map<String, String> placeholders,
        int protocol,
        IoriCorePlugin plugin
    ) {
        if (source == null || source.isEmpty()) return;
        for (String line : source) {
            target.add(color(line, animationConfig, serverTicks, placeholders, protocol, plugin));
        }
    }

    private static void appendLines(
        List<String> source,
        FileConfiguration animationConfig,
        long serverTicks,
        Map<String, String> placeholders,
        int protocol,
        IoriCorePlugin plugin,
        List<String> target
    ) {
        appendLines(target, source, animationConfig, serverTicks, placeholders, protocol, plugin);
    }

    private static String color(
        String template,
        FileConfiguration config,
        long serverTicks,
        Map<String, String> placeholders,
        int protocol,
        IoriCorePlugin plugin
    ) {
        String resolved = resolveAnimationTokens(template, config, serverTicks, placeholders, protocol, plugin);
        return ColorUtil.translate(substitute(resolved, placeholders, plugin), protocol);
    }

    public static String substitute(String template, Map<String, String> placeholders) {
        return substitute(template, placeholders, null);
    }

    public static String substitute(String template, Map<String, String> placeholders, IoriCorePlugin plugin) {
        if (template == null) return "";
        String value = template;
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
        value = applyTimeTokens(value);
        value = applyServerTokens(value, plugin);
        return value;
    }

    private static String applyTimeTokens(String value) {
        if (value == null || !value.toLowerCase(Locale.ROOT).contains("<time")) return value == null ? "" : value;
        Matcher matcher = TIME_TOKEN.matcher(value);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String pattern = matcher.group(1);
            String fmt = pattern == null || pattern.trim().isEmpty() ? "HH:mm:ss" : pattern.trim();
            String formatted;
            try {
                formatted = new SimpleDateFormat(fmt, Locale.ROOT).format(new Date());
            } catch (IllegalArgumentException ex) {
                formatted = new SimpleDateFormat("HH:mm:ss", Locale.ROOT).format(new Date());
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(formatted));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String applyServerTokens(String value, IoriCorePlugin plugin) {
        if (value == null || plugin == null || !value.toLowerCase(Locale.ROOT).contains("<server_")) return value;
        String localName = localServerName(plugin);
        Matcher matcher = SERVER_TOKEN.matcher(value);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String server = matcher.group(1).trim();
            String field = matcher.group(2);
            matcher.appendReplacement(
                buffer,
                Matcher.quoteReplacement(resolveServerField(plugin, localName, server, field))
            );
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String localServerName(IoriCorePlugin plugin) {
        String configured = plugin.settings().getString("SETTINGS.SERVER-NAME", plugin.serverName());
        return configured == null ? "" : configured.trim();
    }

    private static boolean isLocalServer(String localName, String tokenName) {
        if (localName.isEmpty() || tokenName == null || tokenName.isEmpty()) return false;
        return localName.equalsIgnoreCase(tokenName);
    }

    private static String resolveServerField(IoriCorePlugin plugin, String localName, String server, String field) {
        boolean local = isLocalServer(localName, server);
        if (field == null || field.isEmpty()) {
            return local ? "Online" : "Offline";
        }
        String kind = field.toLowerCase(Locale.ROOT);
        if (kind.equals("status")) return local ? "Online" : "Offline";
        if (!local) return "-";
        if (kind.equals("online")) return String.valueOf(VanishVisibility.visibleOnlineCount(plugin, null));
        if (kind.equals("max")) return String.valueOf(Bukkit.getMaxPlayers());
        if (kind.equals("version")) return Bukkit.getBukkitVersion();
        return "-";
    }

    @Getter
    @Accessors(fluent = true)
    public static final class YamlAnimation {

        private final boolean enabled;
        private final int intervalTicks;
        private final List<String> stringFrames;
        private final List<List<String>> listFrames;

        private YamlAnimation(
            boolean enabled,
            int intervalTicks,
            List<String> stringFrames,
            List<List<String>> listFrames
        ) {
            this.enabled = enabled;
            this.intervalTicks = intervalTicks;
            this.stringFrames = stringFrames;
            this.listFrames = listFrames;
        }

        static YamlAnimation from(ConfigurationSection section) {
            if (section == null) {
                return new YamlAnimation(false, 2, Collections.emptyList(), Collections.emptyList());
            }
            List<String> strings = new ArrayList<>();
            List<List<String>> lists = new ArrayList<>();
            List<?> raw = section.getList("FRAMES");
            if (raw != null) {
                for (Object frame : raw) {
                    if (frame instanceof String) {
                        strings.add((String) frame);
                        lists.add(Collections.singletonList((String) frame));
                    } else if (frame instanceof List) {
                        List<String> block = new ArrayList<>();
                        for (Object line : (List<?>) frame) {
                            if (line != null) block.add(String.valueOf(line));
                        }
                        if (!block.isEmpty()) {
                            lists.add(block);
                            strings.add(block.get(0));
                        }
                    }
                }
            }
            boolean enabled = section.getBoolean("ENABLED", !strings.isEmpty());
            int interval = Math.max(1, section.getInt("INTERVAL", 2));
            return new YamlAnimation(enabled, interval, strings, lists);
        }
    }
}
