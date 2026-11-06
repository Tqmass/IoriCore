package us.iorilabs.bukkit.modules.core;

import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

public final class EssentialsLocations {

    private EssentialsLocations() {}

    public static void write(ConfigurationSection section, String path, Location location) {
        if (section == null || location == null || location.getWorld() == null) return;
        section.set(path + ".world", location.getWorld().getName());
        section.set(path + ".x", location.getX());
        section.set(path + ".y", location.getY());
        section.set(path + ".z", location.getZ());
        section.set(path + ".yaw", location.getYaw());
        section.set(path + ".pitch", location.getPitch());
    }

    public static Location read(ConfigurationSection section, String path) {
        if (section == null) return null;
        ConfigurationSection node = section.getConfigurationSection(path);
        if (node == null) return null;

        String worldName = node.getString("world");
        if (worldName == null || worldName.trim().isEmpty()) return null;

        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;

        return new Location(
            world,
            node.getDouble("x"),
            node.getDouble("y"),
            node.getDouble("z"),
            (float) node.getDouble("yaw"),
            (float) node.getDouble("pitch")
        );
    }

    public static String normalizeKey(String name) {
        if (name == null) return "";
        return name.trim().toLowerCase(Locale.ROOT);
    }
}
