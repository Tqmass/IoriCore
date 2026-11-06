package us.iorilabs.proxy.config;

import java.util.List;
import java.util.Locale;

public final class DonationsConfig {

    private final YamlConfig yaml;

    public DonationsConfig(YamlConfig yaml) {
        this.yaml = yaml;
    }

    public String storeUrl() {
        return yaml.getString("store_url", "https://tienda.IoriLabs.net");
    }

    public String defaultSkin() {
        return yaml.getString("default_skin", "Steve");
    }

    public boolean goalEnabled() {
        return yaml.getBoolean("goal.enabled", true);
    }

    public int goalMetaFallback() {
        return Math.max(1, yaml.getInt("goal.meta_fallback", yaml.getInt("goal.meta", 500)));
    }

    public int barLength() {
        return Math.max(1, yaml.getInt("goal.bar_length", 20));
    }

    public boolean ggWaveEnabled() {
        return yaml.getBoolean("ggwave.enabled", true);
    }

    public int ggWaveDurationSeconds() {
        return Math.max(5, yaml.getInt("ggwave.duration_seconds", 15));
    }

    public int ggWaveMaxPerPlayer() {
        return Math.max(1, yaml.getInt("ggwave.max_per_player", 1));
    }

    public int ggWaveMaxTotal() {
        return Math.max(1, yaml.getInt("ggwave.max_total", 40));
    }

    public List<String> ggWaveInputs() {
        List<String> inputs = yaml.getStringList("ggwave.inputs");
        if (!inputs.isEmpty()) {
            return inputs
                .stream()
                .map(s -> s.toLowerCase(Locale.ROOT))
                .toList();
        }

        return List.of(yaml.getString("ggwave.trigger_word", "gg").toLowerCase(Locale.ROOT));
    }

    public boolean ggWaveRainbow() {
        return yaml.getBoolean("ggwave.rainbow", true);
    }

    public boolean ggWaveRainbowOnlyMessage() {
        return yaml.getBoolean("ggwave.rainbow_only_message", true);
    }

    public boolean tebexEnabled() {
        return yaml.getBoolean("tebex.enabled", false);
    }

    public String tebexSecret() {
        return yaml.getString("tebex.secret", "");
    }

    public boolean builtByBitEnabled() {
        return yaml.getBoolean("builtbybit.enabled", false);
    }

    public String builtByBitSecret() {
        return yaml.getString("builtbybit.secret", "");
    }

    public boolean craftingStoreEnabled() {
        return yaml.getBoolean("craftingstore.enabled", false);
    }

    public String craftingStoreSecret() {
        return yaml.getString("craftingstore.secret", "");
    }

    public boolean soundsEnabled() {
        return yaml.getBoolean("sounds.enabled", true);
    }

    public String legacySound() {
        return yaml.getString("sounds.legacy", "random.levelup").trim();
    }

    public String modernSound() {
        return yaml.getString("sounds.modern", "entity.player.levelup").trim();
    }

    public float soundVolume() {
        return Math.max(0f, (float) yaml.getDouble("sounds.volume", 1.0));
    }

    public float soundPitch() {
        return Math.max(0f, (float) yaml.getDouble("sounds.pitch", 1.0));
    }
}
