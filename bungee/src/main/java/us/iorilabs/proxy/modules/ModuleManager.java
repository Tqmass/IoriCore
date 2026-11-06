package us.iorilabs.proxy.modules;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import us.iorilabs.proxy.IoriProxy;
import us.iorilabs.proxy.logging.ProxyLogger;
import us.iorilabs.proxy.modules.core.CoreModule;

public final class ModuleManager {

    private final ProxyLogger logger;
    private final Map<String, ProxyModule> modules = new LinkedHashMap<>();
    private final List<String> enabled = new ArrayList<>();

    public ModuleManager(ProxyLogger logger) {
        this.logger = logger;
    }

    public void registerDefaults(IoriProxy proxy) {
        register(new CoreModule());
    }

    public void register(ProxyModule module) {
        modules.put(module.id(), module);
    }

    public void enableAll(IoriProxy proxy) {
        enabled.clear();

        for (ProxyModule module : modules.values()) {
            boolean toggle = proxy.configs().settings().moduleEnabled(module.id());
            if (!toggle) {
                logger.module(module.id(), "disabled in settings.yml");
                continue;
            }

            try {
                module.onEnable(proxy);
                enabled.add(module.id());
                logger.module(module.id(), "&aenabled");
            } catch (Exception ex) {
                logger.error("Failed to enable module " + module.id(), ex);
            }
        }
    }

    public void disableAll() {
        for (int i = enabled.size() - 1; i >= 0; i--) {
            ProxyModule module = modules.get(enabled.get(i));
            if (module != null) {
                try {
                    module.onDisable(IoriProxy.get());
                } catch (Exception ex) {
                    logger.error("Failed to disable module " + module.id(), ex);
                }
            }
        }

        enabled.clear();
    }

    public void reloadAll(IoriProxy proxy) {
        for (String id : enabled) {
            ProxyModule module = modules.get(id);
            if (module != null) {
                module.onReload(proxy);
            }
        }
    }

    public List<String> enabledModules() {
        return List.copyOf(enabled);
    }
}
