package us.iorilabs.proxy.config;

import java.util.List;

public final class AnnouncementsConfig {

    private final YamlConfig yaml;

    public AnnouncementsConfig(YamlConfig yaml) {
        this.yaml = yaml;
    }

    public int intervalSeconds() {
        return yaml.getInt("interval_seconds", 180);
    }

    public boolean random() {
        return yaml.getBoolean("random", true);
    }

    public List<String> servers() {
        return yaml.getStringList("servers");
    }

    public boolean soundEnabled() {
        return yaml.getBoolean("sound.enabled", true);
    }

    public String legacySound() {
        return yaml.getString("sound.legacy", "note.pling").trim();
    }

    public String modernSound() {
        return yaml.getString("sound.modern", "block.note_block.pling").trim();
    }

    public float soundVolume() {
        return Math.max(0f, (float) yaml.getDouble("sound.volume", 1.0));
    }

    public float soundPitch() {
        return Math.max(0f, (float) yaml.getDouble("sound.pitch", 1.0));
    }
}
