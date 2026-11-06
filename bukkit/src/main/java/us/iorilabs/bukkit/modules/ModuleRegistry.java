package us.iorilabs.bukkit.modules;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.bukkit.configuration.file.FileConfiguration;
import us.iorilabs.api.modules.ModuleId;
import us.iorilabs.bukkit.logging.BukkitLogger;

public final class ModuleRegistry {

    private final Map<ModuleId, CoreModule> modules = new EnumMap<>(ModuleId.class);
    private final Set<ModuleId> active = EnumSet.noneOf(ModuleId.class);
    private final BukkitLogger logger;

    public ModuleRegistry(BukkitLogger logger) {
        this.logger = logger;
    }

    public void register(CoreModule module) {
        modules.put(module.id().canonical(), module);
    }

    public void enableAll() {
        java.util.List<String> enabled = new java.util.ArrayList<>();
        for (CoreModule module : modules.values()) {
            if (!module.enabled()) continue;
            try {
                module.enable();
                active.add(module.id().canonical());
                enabled.add(module.id().name());
            } catch (Exception ex) {
                logger.warn("Module failed · " + module.id().name() + ": " + ex.getMessage());
            }
        }
        if (!enabled.isEmpty()) {
            logger.info("Modules · &f" + enabled.size() + " &7enabled &8· &7" + String.join("&8, &7", enabled));
        }
    }

    public void disableAll() {
        for (CoreModule module : modules.values()) {
            try {
                module.disable();
            } catch (Exception ignored) {}
        }
        active.clear();
    }

    public int reloadAll() {
        return reloadAll(null);
    }

    public int reloadAll(FileConfiguration settings) {
        int reloaded = 0;
        for (CoreModule module : modules.values()) {
            ModuleId id = module.id().canonical();
            boolean want = settings == null ? module.enabled() : ModuleToggle.enabled(settings, id);
            boolean running = active.contains(id);
            try {
                if (want && !running) {
                    module.enable();
                    active.add(id);
                    reloaded++;
                    logger.module(id.name(), "enabled");
                } else if (!want && running) {
                    module.disable();
                    active.remove(id);
                    logger.module(id.name(), "disabled");
                } else if (want && running) {
                    module.reload();
                    reloaded++;
                }
            } catch (Exception ex) {
                logger.warn("Module reload failed · " + id.name() + ": " + ex.getMessage());
            }
        }
        return reloaded;
    }

    public Collection<CoreModule> all() {
        return Collections.unmodifiableCollection(modules.values());
    }

    @SuppressWarnings("unchecked")
    public <T extends CoreModule> T get(ModuleId id) {
        if (id == null) return null;
        return (T) modules.get(id.canonical());
    }
}
