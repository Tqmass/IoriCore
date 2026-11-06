package us.iorilabs.bukkit.modules.staffmode.inventory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.storage.StoragePaths;

public final class StaffInventoryStore {

    private final IoriCorePlugin plugin;
    private final File folder;
    private final ExecutorService io = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "IoriCore-StaffInventory");
        thread.setDaemon(true);
        return thread;
    });

    public StaffInventoryStore(IoriCorePlugin plugin) {
        this.plugin = plugin;
        File legacy = new File(plugin.getDataFolder(), "data/staffmode");
        this.folder = StoragePaths.storage(plugin, "staffmode");
        StoragePaths.migrateIfPresent(legacy, folder);
    }

    public void save(UUID id, StaffSnapshot snapshot) {
        if (id == null || snapshot == null) return;
        String data = encode(snapshot);
        File file = file(id);
        io.execute(() -> write(file, data));
    }

    public void delete(UUID id) {
        if (id == null) return;
        File file = file(id);
        io.execute(() -> {
            if (file.exists() && !file.delete()) plugin.getLogger().warning("Could not delete " + file.getName());
        });
    }

    public void load(UUID id, Consumer<StaffSnapshot> callback) {
        if (id == null || callback == null) return;
        File file = file(id);
        io.execute(() -> {
            if (!file.exists()) return;
            String data;
            try {
                data = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            } catch (IOException exception) {
                plugin.getLogger().log(Level.WARNING, "Could not read " + file.getName(), exception);
                return;
            }
            if (!plugin.isEnabled()) return;
            Bukkit.getScheduler().runTask(plugin, () -> {
                StaffSnapshot snapshot = decode(data);
                if (snapshot != null) callback.accept(snapshot);
            });
        });
    }

    public void shutdown() {
        io.shutdown();
        try {
            if (!io.awaitTermination(5, TimeUnit.SECONDS)) io.shutdownNow();
        } catch (InterruptedException exception) {
            io.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private File file(UUID id) {
        return new File(folder, id + ".yml");
    }

    private void write(File file, String data) {
        try {
            if (!folder.exists() && !folder.mkdirs()) return;
            File temp = new File(folder, file.getName() + ".tmp");
            Files.write(temp.toPath(), data.getBytes(StandardCharsets.UTF_8));
            Files.move(
                temp.toPath(),
                file.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            );
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Could not write " + file.getName(), exception);
        }
    }

    private static String encode(StaffSnapshot snapshot) {
        YamlConfiguration yaml = new YamlConfiguration();
        writeItems(yaml, "CONTENTS", snapshot.contents());
        writeItems(yaml, "ARMOR", snapshot.armor());
        yaml.set("ARMOR-SIZE", snapshot.armor().length);
        yaml.set("CONTENTS-SIZE", snapshot.contents().length);

        Location location = snapshot.location();
        if (location != null && location.getWorld() != null) {
            yaml.set("LOCATION.WORLD", location.getWorld().getName());
            yaml.set("LOCATION.X", location.getX());
            yaml.set("LOCATION.Y", location.getY());
            yaml.set("LOCATION.Z", location.getZ());
            yaml.set("LOCATION.YAW", (double) location.getYaw());
            yaml.set("LOCATION.PITCH", (double) location.getPitch());
        }

        yaml.set("HEALTH", snapshot.health());
        yaml.set("FOOD", snapshot.food());
        yaml.set("SATURATION", (double) snapshot.saturation());
        yaml.set("EXP", (double) snapshot.exp());
        yaml.set("LEVEL", snapshot.level());
        yaml.set("GAMEMODE", snapshot.gameMode() == null ? null : snapshot.gameMode().name());
        yaml.set("ALLOW-FLIGHT", snapshot.allowFlight());
        yaml.set("FLYING", snapshot.flying());
        yaml.set("VANISHED", snapshot.vanished());

        List<String> effects = new ArrayList<>();
        for (PotionEffect effect : snapshot.effects()) {
            effects.add(
                effect.getType().getName() +
                    ":" +
                    effect.getDuration() +
                    ":" +
                    effect.getAmplifier() +
                    ":" +
                    effect.isAmbient()
            );
        }
        yaml.set("EFFECTS", effects);
        return yaml.saveToString();
    }

    private StaffSnapshot decode(String data) {
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.loadFromString(data);
        } catch (InvalidConfigurationException exception) {
            plugin.getLogger().log(Level.WARNING, "Corrupt staff inventory snapshot", exception);
            return null;
        }

        StaffSnapshot snapshot = new StaffSnapshot();
        snapshot.contents(readItems(yaml, "CONTENTS", yaml.getInt("CONTENTS-SIZE", 36)));
        snapshot.armor(readItems(yaml, "ARMOR", yaml.getInt("ARMOR-SIZE", 4)));

        World world = yaml.isString("LOCATION.WORLD") ? Bukkit.getWorld(yaml.getString("LOCATION.WORLD")) : null;
        if (world != null) {
            snapshot.location(
                new Location(
                    world,
                    yaml.getDouble("LOCATION.X"),
                    yaml.getDouble("LOCATION.Y"),
                    yaml.getDouble("LOCATION.Z"),
                    (float) yaml.getDouble("LOCATION.YAW"),
                    (float) yaml.getDouble("LOCATION.PITCH")
                )
            );
        }

        snapshot.health(yaml.getDouble("HEALTH", 20.0D));
        snapshot.food(yaml.getInt("FOOD", 20));
        snapshot.saturation((float) yaml.getDouble("SATURATION", 5.0D));
        snapshot.exp((float) yaml.getDouble("EXP", 0.0D));
        snapshot.level(yaml.getInt("LEVEL", 0));
        snapshot.gameMode(parseGameMode(yaml.getString("GAMEMODE")));
        snapshot.allowFlight(yaml.getBoolean("ALLOW-FLIGHT", false));
        snapshot.flying(yaml.getBoolean("FLYING", false));
        snapshot.vanished(yaml.getBoolean("VANISHED", false));

        List<PotionEffect> effects = new ArrayList<>();
        for (String raw : yaml.getStringList("EFFECTS")) {
            PotionEffect effect = parseEffect(raw);
            if (effect != null) effects.add(effect);
        }
        snapshot.effects(effects);
        return snapshot;
    }

    private static void writeItems(YamlConfiguration yaml, String path, ItemStack[] items) {
        for (int i = 0; i < items.length; i++) {
            if (items[i] != null) yaml.set(path + "." + i, items[i]);
        }
    }

    private static ItemStack[] readItems(YamlConfiguration yaml, String path, int size) {
        ItemStack[] items = new ItemStack[Math.max(0, size)];
        ConfigurationSection section = yaml.getConfigurationSection(path);
        if (section == null) return items;
        for (String key : section.getKeys(false)) {
            int slot;
            try {
                slot = Integer.parseInt(key);
            } catch (NumberFormatException ignored) {
                continue;
            }
            if (slot < 0 || slot >= items.length) continue;
            items[slot] = section.getItemStack(key);
        }
        return items;
    }

    private static GameMode parseGameMode(String raw) {
        if (raw == null) return null;
        try {
            return GameMode.valueOf(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static PotionEffect parseEffect(String raw) {
        if (raw == null) return null;
        String[] parts = raw.split(":");
        if (parts.length < 3) return null;
        PotionEffectType type = PotionEffectType.getByName(parts[0]);
        if (type == null) return null;
        try {
            int duration = Integer.parseInt(parts[1]);
            int amplifier = Integer.parseInt(parts[2]);
            boolean ambient = parts.length > 3 && Boolean.parseBoolean(parts[3]);
            return new PotionEffect(type, duration, amplifier, ambient);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
