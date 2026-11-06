package us.iorilabs.proxy.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import us.iorilabs.proxy.logging.ProxyLogger;

final class SettingsDiscordMigration {

    private static final DateTimeFormatter BAK_TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private SettingsDiscordMigration() {}

    static boolean run(YamlConfig settingsYaml, YamlConfig discordYaml, ProxyLogger logger) {
        Map<String, Object> sRoot = settingsYaml.root();
        Object staffBlock = sRoot.remove("STAFF-AUTH");
        Object legacyDiscord = sRoot.remove("DISCORD");

        if (staffBlock == null && legacyDiscord == null) {
            return false;
        }

        Map<String, Object> dRoot = discordYaml.root();

        if (staffBlock != null) {
            dRoot.putIfAbsent("STAFF-AUTH", staffBlock);
            logger.warn("Migrated STAFF-AUTH from settings.yml → discord.yml (single source: discord.yml)");
        }

        if (legacyDiscord instanceof Map<?, ?> dm) {
            mergeLegacyDiscordBot(castMap(dm), dRoot);
            logger.warn(
                "Migrated DISCORD from settings.yml → discord.yml (bot switch: discord.yml ENABLED + BOTS.PRIMARY)"
            );
        }

        backup(settingsYaml.path(), logger);
        backup(discordYaml.path(), logger);
        settingsYaml.save();
        discordYaml.save();
        return true;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Map<?, ?> m) {
        return (Map<String, Object>) m;
    }

    private static void mergeLegacyDiscordBot(Map<String, Object> legacy, Map<String, Object> discRoot) {
        Object botObj = legacy.get("BOT");
        if (!(botObj instanceof Map<?, ?> botRaw)) return;

        Map<String, Object> bot = castMap(botRaw);
        Map<String, Object> bots = section(discRoot, "BOTS");
        Map<String, Object> primary = section(bots, "PRIMARY");

        putIfEmpty(primary, "TOKEN", bot.get("TOKEN"));
        putIfEmpty(primary, "GUILD-ID", bot.get("GUILD-ID"));
        primary.putIfAbsent("ENABLED", bot.get("ENABLED"));

        Map<String, Object> looseBot = section(discRoot, "BOT");
        copyIfAbsent(looseBot, bot, "PREFIX");
        copyIfAbsent(looseBot, bot, "SEND-WEBHOOK-EVENTS");
        copyIfAbsent(looseBot, bot, "SLASH-COMMANDS");
        copyIfAbsent(looseBot, bot, "PREFIX-COMMANDS");

        Object ch = bot.get("CHANNELS");
        if (ch instanceof Map<?, ?> chm) {
            Map<String, Object> targetCh = section(discRoot, "CHANNELS");
            for (Map.Entry<?, ?> e : chm.entrySet()) {
                String k = YamlConfig.normalizeKey(String.valueOf(e.getKey()));
                targetCh.putIfAbsent(k, e.getValue());
            }
        }

        Object cmds = bot.get("COMMANDS");
        if (cmds instanceof Map<?, ?> cm) {
            Map<String, Object> targetCmd = section(discRoot, "COMMANDS");
            for (Map.Entry<?, ?> e : cm.entrySet()) {
                String k = YamlConfig.normalizeKey(String.valueOf(e.getKey()));
                targetCmd.putIfAbsent(k, e.getValue());
            }
        }
    }

    private static Map<String, Object> section(Map<String, Object> root, String key) {
        String nk = YamlConfig.normalizeKey(key);
        Object cur = root.get(nk);
        if (cur instanceof Map<?, ?> m) {
            return castMap(m);
        }
        Map<String, Object> created = new LinkedHashMap<>();
        root.put(nk, created);
        return created;
    }

    private static void putIfEmpty(Map<String, Object> map, String key, Object v) {
        if (v == null) return;
        String nk = YamlConfig.normalizeKey(key);
        Object cur = map.get(nk);
        if (!map.containsKey(nk) || (cur instanceof String s && s.isBlank())) {
            map.put(nk, v);
        }
    }

    private static void copyIfAbsent(Map<String, Object> dest, Map<String, Object> src, String key) {
        String nk = YamlConfig.normalizeKey(key);
        if (!dest.containsKey(nk) && src.containsKey(nk)) {
            dest.put(nk, src.get(nk));
        }
    }

    private static void backup(Path path, ProxyLogger logger) {
        try {
            if (!Files.exists(path)) return;
            Path bak = path.resolveSibling(path.getFileName() + ".bak-" + BAK_TS.format(LocalDateTime.now()));
            Files.copy(path, bak, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            logger.warn("Config backup failed (" + path + "): " + ex.getMessage());
        }
    }
}
