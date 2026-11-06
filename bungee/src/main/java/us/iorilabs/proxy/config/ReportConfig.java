package us.iorilabs.proxy.config;

public final class ReportConfig {

    private final SettingsConfig settings;

    public ReportConfig(SettingsConfig settings) {
        this.settings = settings;
    }

    public int cooldownSeconds() {
        return settings.cooldownSeconds("report", 30);
    }
}
