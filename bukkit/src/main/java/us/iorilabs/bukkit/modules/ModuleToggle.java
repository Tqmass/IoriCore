package us.iorilabs.bukkit.modules;

import org.bukkit.configuration.file.FileConfiguration;
import us.iorilabs.api.modules.ModuleId;

public final class ModuleToggle {

    private ModuleToggle() {}

    public static boolean enabled(FileConfiguration settings, ModuleId id) {
        if (settings == null || id == null) return false;
        ModuleId canonical = id.canonical();
        if (canonical == ModuleId.CORE) {
            if (settings.contains("MODULES.CORE")) return settings.getBoolean("MODULES.CORE");
            return settings.getBoolean("MODULES.ESSENTIALS", true);
        }
        return settings.getBoolean("MODULES." + canonical.modulesConfigKey(), defaultEnabled(canonical));
    }

    private static boolean defaultEnabled(ModuleId id) {
        return switch (id) {
            case ECONOMY, CHAT_GAMES, TAGS, COINSHOP, STAFF_AUTH, DISCORD, GENDER -> false;
            default -> true;
        };
    }
}
