package us.iorilabs.bukkit.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public final class LegacyMaterials {

    private static final String[] DYES = {
        "WHITE",
        "ORANGE",
        "MAGENTA",
        "LIGHT_BLUE",
        "YELLOW",
        "LIME",
        "PINK",
        "GRAY",
        "LIGHT_GRAY",
        "CYAN",
        "PURPLE",
        "BLUE",
        "BROWN",
        "GREEN",
        "RED",
        "BLACK",
    };

    private LegacyMaterials() {}

    public static Material of(String... names) {
        if (names == null) return Material.STONE;
        for (String name : names) {
            if (name == null || name.trim().isEmpty()) continue;
            try {
                return Material.valueOf(name.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }
        return Material.STONE;
    }

    public static Material grayPane() {
        return of("GRAY_STAINED_GLASS_PANE", "STAINED_GLASS_PANE", "THIN_GLASS", "GLASS");
    }

    public static Material whitePane() {
        return of("WHITE_STAINED_GLASS_PANE", "STAINED_GLASS_PANE", "THIN_GLASS", "GLASS");
    }

    public static Material playerHead() {
        return of("PLAYER_HEAD", "SKULL_ITEM", "SKULL");
    }

    public static ItemStack stainedPane(int legacyData) {
        return dyed("STAINED_GLASS_PANE", legacyData);
    }

    public static ItemStack wool(int legacyData) {
        return dyed("WOOL", legacyData);
    }

    @SuppressWarnings("deprecation")
    private static ItemStack dyed(String suffix, int legacyData) {
        int index = Math.floorMod(legacyData, DYES.length);
        Material modern = Material.getMaterial(DYES[index] + "_" + suffix);
        if (modern != null) return new ItemStack(modern);

        Material legacy = Material.getMaterial(suffix);
        if (legacy == null) return new ItemStack(Material.STONE);
        return new ItemStack(legacy, 1, (short) index);
    }

    public static boolean isPlayerHead(Material material) {
        if (material == null) return false;
        String name = material.name();
        return (
            "PLAYER_HEAD".equals(name) ||
            "PLAYER_WALL_HEAD".equals(name) ||
            "SKULL_ITEM".equals(name) ||
            "SKULL".equals(name)
        );
    }
}
