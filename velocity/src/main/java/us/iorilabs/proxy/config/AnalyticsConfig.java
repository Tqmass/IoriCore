package us.iorilabs.proxy.config;

public final class AnalyticsConfig {

    private final YamlConfig yaml;

    public AnalyticsConfig(YamlConfig yaml) {
        this.yaml = yaml;
    }

    public boolean persistDailySql() {
        return yaml.getBoolean("persist_daily_sql", true);
    }
}
