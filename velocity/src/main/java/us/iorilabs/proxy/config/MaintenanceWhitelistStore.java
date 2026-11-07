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

    private final MaintenanceConfig maintenanceConfig;
    private final ProxyLogger logger;

    public MaintenanceWhitelistStore(ProxyPlatform platform, ProxyLogger logger, MaintenanceConfig maintenanceConfig) {
        this.logger = logger;
        this.maintenanceConfig = maintenanceConfig;
        this.yaml = new YamlConfig(platform, logger, DATA_FILE);
    }

    public List<String> loadAndMigrate() {
        yaml.load();
        List<String> entries = new ArrayList<>(yaml.getStringList("entries"));
        if (!entries.isEmpty()) return entries;

        List<String> legacy = maintenanceConfig.legacyWhitelist();
        if (legacy.isEmpty()) return Collections.emptyList();

        entries = new ArrayList<>(legacy);
        save(entries);
        maintenanceConfig.yaml().saveTopLevelList("WHITELIST", List.of());
        logger.info("Maintenance whitelist migrated from maintenance.yml → " + DATA_FILE);
        return entries;
    }

    public void save(Collection<String> entries) {
        yaml.saveTopLevelList("ENTRIES", new ArrayList<>(entries));
    }
}
