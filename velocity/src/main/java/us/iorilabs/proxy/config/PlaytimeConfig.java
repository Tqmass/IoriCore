package us.iorilabs.proxy.config;

public final class PlaytimeConfig {

    private final YamlConfig yaml;

    public PlaytimeConfig(YamlConfig settingsYaml) {
        this.yaml = settingsYaml;
    }

    public int flushIntervalSeconds() {
        return Math.max(15, yaml.getInt("playtime.flush_interval_seconds", 60));
    }

    public boolean memoryFallback() {
        return yaml.getBoolean("playtime.memory_fallback", true);
    }
}
