package us.iorilabs.proxy.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import lombok.Getter;
import lombok.experimental.Accessors;
import us.iorilabs.proxy.logging.ProxyLogger;
import us.iorilabs.proxy.platform.ProxyPlatform;

@Accessors(fluent = true)
public final class MaintenanceWhitelistStore {

    public static final String DATA_FILE = "data/whitelist.yml";

    @Getter
    private final YamlConfig yaml;

    private final ProxyLogger logger;

    public MaintenanceWhitelistStore(ProxyPlatform platform, ProxyLogger logger, MaintenanceConfig maintenanceConfig) {
        this.logger = logger;
        this.yaml = new YamlConfig(platform, logger, DATA_FILE);
    }

    public List<String> load() {
        yaml.load();
        List<String> entries = yaml.getStringList("ENTRIES");
        if (entries == null || entries.isEmpty()) return Collections.emptyList();
        return new ArrayList<>(entries);
    }

    public void save(Collection<String> entries) {
        yaml.saveTopLevelList("ENTRIES", new ArrayList<>(entries));
    }
}
