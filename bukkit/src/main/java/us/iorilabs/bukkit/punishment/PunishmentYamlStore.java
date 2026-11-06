package us.iorilabs.bukkit.punishment;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import us.iorilabs.api.punishment.PunishmentType;
import us.iorilabs.bukkit.punishment.enums.PunishmentStatus;

public final class PunishmentYamlStore {

    private final File file;
    private final Logger logger;
    private final AtomicLong nextId = new AtomicLong(1L);
    private FileConfiguration yaml = new YamlConfiguration();

    public PunishmentYamlStore(File dataFolder, Logger logger) {
        File dir = new File(dataFolder, "data");
        if (!dir.exists()) dir.mkdirs();
        this.file = new File(dir, "punishments.yml");
        this.logger = logger;
        load();
    }

    public synchronized void load() {
        if (!file.exists()) {
            yaml = new YamlConfiguration();
            yaml.set("NEXT-ID", 1L);
            yaml.createSection("ENTRIES");
            saveQuiet();
            return;
        }
        yaml = YamlConfiguration.loadConfiguration(file);
        nextId.set(Math.max(1L, yaml.getLong("NEXT-ID", 1L)));
    }

    public synchronized Optional<Punishment> load(long id) {
        if (id <= 0L) return Optional.empty();
        ConfigurationSection section = yaml.getConfigurationSection("ENTRIES." + id);
        if (section == null) return Optional.empty();
        return Optional.ofNullable(map(section, id));
    }

    public synchronized Optional<Punishment> loadActiveBan(UUID targetId) {
        return loadActive(targetId, PunishmentType.BAN, PunishmentType.TEMPBAN);
    }

    public synchronized Optional<Punishment> loadActiveMute(UUID targetId) {
        return loadActive(targetId, PunishmentType.MUTE, PunishmentType.TEMPMUTE);
    }

    public synchronized Optional<Punishment> loadActiveWarn(UUID targetId) {
        return loadActive(targetId, PunishmentType.WARN, PunishmentType.WARN);
    }

    public synchronized void upsert(Punishment punishment) {
        if (punishment == null) return;

        if (punishment.id() <= 0L) {
            punishment.id(nextId.getAndIncrement());
            yaml.set("NEXT-ID", nextId.get());
        }

        String path = "ENTRIES." + punishment.id();
        yaml.set(path + ".TYPE", punishment.type() == null ? PunishmentType.WARN.name() : punishment.type().name());
        yaml.set(
            path + ".STATUS",
            punishment.status() == null ? PunishmentStatus.ACTIVE.name() : punishment.status().name()
        );
        yaml.set(path + ".TARGET-UUID", punishment.targetId() == null ? "" : punishment.targetId().toString());
        yaml.set(path + ".TARGET-NAME", nullToEmpty(punishment.targetName()));
        yaml.set(path + ".ACTOR-UUID", punishment.actorId() == null ? "" : punishment.actorId().toString());
        yaml.set(path + ".ACTOR-NAME", nullToEmpty(punishment.actorName()));
        yaml.set(path + ".REASON", nullToEmpty(punishment.reason()));
        yaml.set(path + ".CREATED-AT", punishment.createdAt());
        yaml.set(path + ".EXPIRES-AT", punishment.expiresAt());
        yaml.set(path + ".SILENT", punishment.silent());
        saveQuiet();
    }

    private Optional<Punishment> loadActive(UUID targetId, PunishmentType primary, PunishmentType secondary) {
        if (targetId == null) return Optional.empty();

        ConfigurationSection entries = yaml.getConfigurationSection("ENTRIES");
        if (entries == null) return Optional.empty();

        Punishment best = null;
        for (String key : entries.getKeys(false)) {
            ConfigurationSection section = entries.getConfigurationSection(key);
            if (section == null) continue;

            long id;
            try {
                id = Long.parseLong(key);
            } catch (NumberFormatException ex) {
                continue;
            }

            Punishment punishment = map(section, id);
            if (punishment == null) continue;
            if (punishment.status() != PunishmentStatus.ACTIVE) continue;
            if (punishment.targetId() == null || !punishment.targetId().equals(targetId)) continue;
            if (punishment.type() != primary && punishment.type() != secondary) continue;
            if (isExpired(punishment)) continue;

            if (best == null || punishment.createdAt() > best.createdAt()) best = punishment;
        }
        return Optional.ofNullable(best);
    }

    private Punishment map(ConfigurationSection section, long id) {
        try {
            PunishmentType type = PunishmentType.valueOf(
                section.getString("TYPE", PunishmentType.WARN.name()).toUpperCase(Locale.ROOT)
            );
            PunishmentStatus status = PunishmentStatus.valueOf(
                section.getString("STATUS", PunishmentStatus.ACTIVE.name()).toUpperCase(Locale.ROOT)
            );
            Punishment punishment = new Punishment();
            punishment
                .id(id)
                .type(type)
                .status(status)
                .targetId(parseUuid(section.getString("TARGET-UUID")))
                .targetName(section.getString("TARGET-NAME", ""))
                .actorId(parseUuid(section.getString("ACTOR-UUID")))
                .actorName(section.getString("ACTOR-NAME", "Console"))
                .reason(section.getString("REASON", "No reason provided"))
                .createdAt(section.getLong("CREATED-AT", 0L))
                .expiresAt(section.getLong("EXPIRES-AT", 0L))
                .silent(section.getBoolean("SILENT", false));
            return punishment;
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Bad punishment YAML entry id=" + id, ex);
            return null;
        }
    }

    private void saveQuiet() {
        try {
            yaml.save(file);
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Failed to save " + file.getName(), ex);
        }
    }

    private static UUID parseUuid(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static boolean isExpired(Punishment punishment) {
        long expires = punishment.expiresAt();
        return expires > 0L && expires <= System.currentTimeMillis();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    public synchronized List<Punishment> all() {
        List<Punishment> out = new ArrayList<>();
        ConfigurationSection entries = yaml.getConfigurationSection("ENTRIES");
        if (entries == null) return out;
        for (String key : entries.getKeys(false)) {
            ConfigurationSection section = entries.getConfigurationSection(key);
            if (section == null) continue;
            try {
                Punishment mapped = map(section, Long.parseLong(key));
                if (mapped != null) out.add(mapped);
            } catch (NumberFormatException ignored) {}
        }
        return out;
    }
}
