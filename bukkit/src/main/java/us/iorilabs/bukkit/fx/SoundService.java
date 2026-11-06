package us.iorilabs.bukkit.fx;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;

public final class SoundService {

    private static final Method PLAYER_PLAY_STRING = resolvePlayerPlayString();
    private static final Method WORLD_PLAY_STRING = resolveWorldPlayString();

    private final IoriCorePlugin plugin;
    private final Map<String, SoundEntry> entries = new ConcurrentHashMap<String, SoundEntry>();
    private final Map<String, ResolvedSound> resolved = new ConcurrentHashMap<String, ResolvedSound>();

    public SoundService(IoriCorePlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        entries.clear();
        resolved.clear();

        File file = new File(plugin.getDataFolder(), "sounds.yml");
        us.iorilabs.bukkit.util.PluginResources.saveDefault(plugin, "sounds.yml");
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("SOUNDS");
        if (root == null) {
            plugin.getLogger().warning("sounds.yml missing SOUNDS section");
            return;
        }

        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) continue;

            List<String> names = new ArrayList<String>();
            List<String> listed = namesList(section);
            if (!listed.isEmpty()) {
                for (String name : listed) {
                    if (name != null && !name.trim().isEmpty()) {
                        names.add(name.trim());
                    }
                }
            } else {
                String single = firstString(section, "NAME", "name");
                if (single != null && !single.trim().isEmpty()) {
                    names.add(single.trim());
                }
            }
            if (names.isEmpty()) continue;

            float volume = (float) number(section, "VOLUME", "volume");
            float pitch = (float) number(section, "PITCH", "pitch");
            String normalized = normalizeKey(key);
            entries.put(normalized, new SoundEntry(normalized, names, volume, pitch));
        }
    }

    public void play(Player player, SoundKey key) {
        if (key == null) return;
        play(player, key.configKey());
    }

    public void play(Player player, String logicalKey) {
        if (player == null || !player.isOnline() || logicalKey == null) return;
        SoundEntry entry = entries.get(normalizeKey(logicalKey));
        if (entry != null) {
            playResolved(player, player.getLocation(), entry);
            return;
        }
        playRaw(player, player.getLocation(), logicalKey.trim(), 1.0F, 1.0F);
    }

    public void playAt(Location location, SoundKey key) {
        if (key == null) return;
        playAt(location, key.configKey());
    }

    public void playAt(Location location, String logicalKey) {
        if (location == null || location.getWorld() == null || logicalKey == null) return;
        SoundEntry entry = entries.get(normalizeKey(logicalKey));
        if (entry != null) {
            ResolvedSound sound = resolve(entry);
            if (sound == null) return;
            if (sound.enumSound != null) {
                location.getWorld().playSound(location, sound.enumSound, entry.volume(), entry.pitch());
                return;
            }
            if (sound.rawName != null && WORLD_PLAY_STRING != null) {
                try {
                    WORLD_PLAY_STRING.invoke(
                        location.getWorld(),
                        location,
                        sound.rawName,
                        Float.valueOf(entry.volume()),
                        Float.valueOf(entry.pitch())
                    );
                } catch (ReflectiveOperationException ex) {
                    plugin.getLogger().log(Level.FINE, "Sound string play failed: " + sound.rawName, ex);
                }
            }
            return;
        }
        playRaw(null, location, logicalKey.trim(), 1.0F, 1.0F);
    }

    private void playRaw(Player player, Location location, String name, float volume, float pitch) {
        if (location == null || name == null || name.isEmpty()) return;
        Sound enumSound = soundEnum(name);
        if (enumSound != null) {
            if (player != null) player.playSound(location, enumSound, volume, pitch);
            else if (location.getWorld() != null) location.getWorld().playSound(location, enumSound, volume, pitch);
            return;
        }
        String raw = toNamespaced(name);
        if (raw == null) return;
        try {
            if (player != null && PLAYER_PLAY_STRING != null) {
                PLAYER_PLAY_STRING.invoke(player, location, raw, Float.valueOf(volume), Float.valueOf(pitch));
            } else if (location.getWorld() != null && WORLD_PLAY_STRING != null) {
                WORLD_PLAY_STRING.invoke(
                    location.getWorld(),
                    location,
                    raw,
                    Float.valueOf(volume),
                    Float.valueOf(pitch)
                );
            }
        } catch (ReflectiveOperationException ex) {
            plugin.getLogger().log(Level.FINE, "Raw sound play failed: " + raw, ex);
        }
    }

    public boolean has(String logicalKey) {
        if (logicalKey == null) return false;
        return entries.containsKey(normalizeKey(logicalKey));
    }

    public Map<String, SoundEntry> entries() {
        return Collections.unmodifiableMap(entries);
    }

    private void playResolved(Player player, Location location, SoundEntry entry) {
        ResolvedSound sound = resolve(entry);
        if (sound == null) return;
        if (sound.enumSound != null) {
            player.playSound(location, sound.enumSound, entry.volume(), entry.pitch());
            return;
        }
        if (sound.rawName != null) {
            playRaw(player, location, sound.rawName, entry.volume(), entry.pitch());
        }
    }

    private ResolvedSound resolve(SoundEntry entry) {
        ResolvedSound cached = resolved.get(entry.key());
        if (cached != null) return cached == ResolvedSound.NONE ? null : cached;

        for (String name : entry.names()) {
            Sound enumSound = soundEnum(name);
            if (enumSound != null) {
                ResolvedSound ok = new ResolvedSound(enumSound, null);
                resolved.put(entry.key(), ok);
                return ok;
            }
        }

        if (PLAYER_PLAY_STRING != null || WORLD_PLAY_STRING != null) {
            for (String name : entry.names()) {
                String raw = toNamespaced(name);
                if (raw == null) continue;
                ResolvedSound ok = new ResolvedSound(null, raw);
                resolved.put(entry.key(), ok);
                return ok;
            }
        }

        resolved.put(entry.key(), ResolvedSound.NONE);
        plugin.getLogger().fine("No sound resolved for key " + entry.key());
        return null;
    }

    private static String normalizeKey(String key) {
        return key.trim().toUpperCase(Locale.ROOT).replace('-', '_');
    }

    private static List<String> namesList(ConfigurationSection section) {
        if (section.isList("NAMES")) return section.getStringList("NAMES");
        if (section.isList("names")) return section.getStringList("names");
        return Collections.emptyList();
    }

    private static String firstString(ConfigurationSection section, String upper, String lower) {
        String value = section.getString(upper);
        return value != null ? value : section.getString(lower);
    }

    private static double number(ConfigurationSection section, String upper, String lower) {
        if (section.contains(upper)) return section.getDouble(upper, 1.0D);
        return section.getDouble(lower, 1.0D);
    }

    private static Sound soundEnum(String name) {
        if (name == null || name.trim().isEmpty()) return null;
        String normalized = name.trim().toUpperCase(Locale.ROOT).replace('.', '_').replace('-', '_');
        try {
            return Sound.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static String toNamespaced(String name) {
        if (name == null || name.trim().isEmpty()) return null;
        String trimmed = name.trim();
        if (trimmed.indexOf(':') >= 0) return trimmed.toLowerCase(Locale.ROOT);
        if (trimmed.indexOf('.') >= 0) return trimmed.toLowerCase(Locale.ROOT);
        return trimmed.toLowerCase(Locale.ROOT).replace('_', '.');
    }

    private static Method resolvePlayerPlayString() {
        try {
            return Player.class.getMethod("playSound", Location.class, String.class, float.class, float.class);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static Method resolveWorldPlayString() {
        try {
            return org.bukkit.World.class.getMethod(
                "playSound",
                Location.class,
                String.class,
                float.class,
                float.class
            );
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static final class ResolvedSound {

        private static final ResolvedSound NONE = new ResolvedSound(null, null);

        private final Sound enumSound;
        private final String rawName;

        private ResolvedSound(Sound enumSound, String rawName) {
            this.enumSound = enumSound;
            this.rawName = rawName;
        }
    }
}
