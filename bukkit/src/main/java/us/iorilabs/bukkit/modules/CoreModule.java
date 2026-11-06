package us.iorilabs.bukkit.modules;

import us.iorilabs.api.modules.ModuleId;

public interface CoreModule {
    ModuleId id();

    boolean enabled();

    void enable();

    void disable();

    default void reload() {
        disable();
        enable();
    }
}
