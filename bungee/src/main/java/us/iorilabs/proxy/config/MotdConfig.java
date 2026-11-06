package us.iorilabs.proxy.config;

import java.util.ArrayList;
import java.util.List;

public final class MotdConfig {

    private final YamlConfig yaml;

    public MotdConfig(YamlConfig yaml) {
        this.yaml = yaml;
    }

    public boolean random() {
        return yaml.getBoolean("random", true);
    }

    public boolean center() {
        return yaml.getBoolean("center", true);
    }

    public String favicon() {
        return yaml.getString("favicon", "");
    }

    public List<String> modernEntries() {
        return multilineEntries("modern");
    }

    public List<String> legacyEntries() {
        return multilineEntries("legacy");
    }

    private List<String> multilineEntries(String path) {
        List<String> raw = yaml.getStringList(path);
        List<String> out = new ArrayList<>();

        for (String entry : raw) {
            if (entry == null || entry.isBlank()) continue;
            out.add(entry.stripTrailing());
        }

        return List.copyOf(out);
    }
}
