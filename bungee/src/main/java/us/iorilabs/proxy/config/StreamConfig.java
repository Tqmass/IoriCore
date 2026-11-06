package us.iorilabs.proxy.config;

public final class StreamConfig {

    private final SettingsConfig settings;

    public StreamConfig(SettingsConfig settings) {
        this.settings = settings;
    }

    public int cooldownSeconds() {
        return settings.cooldownSeconds("stream", 60);
    }
}
