package us.iorilabs.proxy.config;

public final class HelpOpConfig {

    private final SettingsConfig settings;

    public HelpOpConfig(SettingsConfig settings) {
        this.settings = settings;
    }

    public int cooldownSeconds() {
        return settings.cooldownSeconds("helpop", 20);
    }
}
