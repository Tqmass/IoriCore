package us.iorilabs.proxy.config;

import java.util.Locale;
import us.iorilabs.proxy.utilities.ProtocolVersions;

public final class SettingsConfig {

    public static final String ORIGIN_PROXY_ID = "proxy-1";

    private final YamlConfig yaml;
    private final boolean discordBotEnabled;

    public SettingsConfig(YamlConfig yaml, YamlConfig discordYaml) {
        this.yaml = yaml;
        YamlConfig primary = discordYaml.section("BOTS").section("PRIMARY");
        YamlConfig loose = discordYaml.section("BOT");
        boolean primaryOn = primary.getBoolean("ENABLED", false) && !primary.getString("TOKEN", "").isBlank();
        boolean looseOn = loose.getBoolean("ENABLED", false) && !loose.getString("TOKEN", "").isBlank();
        boolean rootOn = discordYaml.getBoolean("ENABLED", false) && !discordYaml.getString("TOKEN", "").isBlank();
        this.discordBotEnabled = primaryOn || looseOn || rootOn;
    }

    public void validate(ConfigValidation validation) {
        if (language().isBlank()) {
            validation.error(yaml.fileName(), "SETTINGS.LANGUAGE", "Language cannot be blank");
        }
    }

    public YamlConfig yaml() {
        return yaml;
    }

    public String proxyId() {
        String id = yaml.getString("settings.proxy-id", "").trim();

        return id.isEmpty() ? ORIGIN_PROXY_ID : id;
    }

    public String language() {
        String nested = yaml.getString("settings.language", "");
        if (!nested.isBlank()) return nested.toLowerCase(Locale.ROOT);

        return yaml.getString("language", "es").toLowerCase(Locale.ROOT);
    }

    public String timezone() {
        return "UTC";
    }

    public boolean debug() {
        if (yaml.contains("settings.debug")) return yaml.getBoolean("settings.debug", false);

        return yaml.getBoolean("debug", false);
    }

    public int legacyProtocolMax() {
        return ProtocolVersions.LEGACY_MAX_PROTOCOL;
    }

    public boolean moduleEnabled(String module) {
        if (module == null || module.isBlank()) return true;

        return yaml.getBoolean("modules." + module.replace('_', '-'), true);
    }

    public boolean discordBotEnabled() {
        return discordBotEnabled;
    }

    public long banCheckCacheSeconds() {
        return Math.max(0, yaml.getInt("punishments.ban-check-cache-seconds", 60));
    }

    public int cooldownSeconds(String key, int defaultSeconds) {
        if (key == null || key.isBlank()) return Math.max(0, defaultSeconds);

        return Math.max(0, yaml.getInt("cooldowns." + key.replace('_', '-'), defaultSeconds));
    }

    public boolean notification(String module, String kind, boolean defaultValue) {
        if (module == null || kind == null) return defaultValue;

        String path = "notifications." + module.replace('_', '-') + "." + kind.replace('_', '-');
        return yaml.getBoolean(path, defaultValue);
    }
}
