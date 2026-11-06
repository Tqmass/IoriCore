package us.iorilabs.proxy.motd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import us.iorilabs.proxy.config.MaintenanceConfig;
import us.iorilabs.proxy.config.MotdConfig;
import us.iorilabs.proxy.config.SettingsConfig;
import us.iorilabs.proxy.language.LanguageManager;
import us.iorilabs.proxy.maintenance.MaintenanceService;
import us.iorilabs.proxy.maintenance.MaintenanceState;
import us.iorilabs.proxy.maintenance.MaintenanceType;
import us.iorilabs.proxy.utilities.DurationParser;
import us.iorilabs.proxy.utilities.TextCenter;

public final class MotdService {

    private MotdConfig config;
    private final MaintenanceService maintenance;
    private final LanguageManager language;
    private final SettingsConfig settings;

    public MotdService(
        MotdConfig config,
        MaintenanceService maintenance,
        LanguageManager language,
        SettingsConfig settings
    ) {
        this.config = config;
        this.maintenance = maintenance;
        this.language = language;
        this.settings = settings;
    }

    public void reloadConfig(MotdConfig config) {
        this.config = config;
    }

    public boolean enabled() {
        return settings.moduleEnabled("motd");
    }

    public String description(int protocol) {
        List<String> maintenanceLines = maintenanceMotdPool(protocol);
        boolean fromMaintenancePool = !maintenanceLines.isEmpty();
        List<String> lines = maintenanceLines;
        if (lines.isEmpty()) {
            if (maintenance.isGlobalActive()) {
                lines = linesFor("MOTD.MAINTENANCE", protocol);
            } else if (protocol > 0 && protocol <= legacyMax()) {
                lines = config.legacyEntries();
                if (lines.isEmpty()) {
                    lines = config.modernEntries();
                }
            } else {
                lines = config.modernEntries();
            }
        }

        if (lines.isEmpty()) return "";

        String picked =
            !config.random() || lines.size() == 1
                ? lines.get(0)
                : lines.get(ThreadLocalRandom.current().nextInt(lines.size()));

        String formatted = language.format(picked, pingPlaceholders());
        // Maintenance MOTDs ya vienen centrados a mano (estilo kennytv); no re-centrar.
        if (config.center() && !fromMaintenancePool) {
            formatted = TextCenter.centerMotd(formatted);
        }

        return formatted;
    }

    public Optional<String> maintenanceCountMessage() {
        if (!maintenance.isGlobalActive() || !maintenance.config().playerListEnabled()) return Optional.empty();

        MaintenanceConfig cfg = maintenance.config();
        boolean timer = hasEndTimer();
        String template =
            timer && !cfg.playerListCountTimerMessage().isBlank()
                ? cfg.playerListCountTimerMessage()
                : cfg.playerListCountMessage();
        if (template.isBlank()) return Optional.empty();

        return Optional.of(language.format(template, pingPlaceholders()));
    }

    public List<String> maintenanceSampleNames() {
        if (!maintenance.isGlobalActive() || !maintenance.config().playerListEnabled()) return List.of();

        MaintenanceConfig cfg = maintenance.config();
        boolean timer = hasEndTimer();
        List<String> hover =
            timer && !cfg.playerListHoverTimer().isEmpty() ? cfg.playerListHoverTimer() : cfg.playerListHover();

        List<String> names = new ArrayList<>();
        Map<String, String> placeholders = pingPlaceholders();

        for (String line : hover) {
            if (line == null || line.isBlank()) continue;
            names.add(language.format(line, placeholders));
        }

        for (String sample : cfg.playerListSamplePlayers()) {
            if (sample == null || sample.isBlank()) continue;
            names.add(language.format(sample, placeholders));
        }

        return names;
    }

    private List<String> maintenanceMotdPool(int protocol) {
        if (!maintenance.isGlobalActive() || !maintenance.config().motdOverride()) return List.of();

        MaintenanceConfig cfg = maintenance.config();
        boolean legacy = protocol > 0 && protocol <= legacyMax();
        boolean timer = cfg.motdShowEndTimer() && hasEndTimer();

        List<String> pool;
        if (timer) {
            pool = legacy ? cfg.motdTimerLegacy() : cfg.motdTimerModern();
            if (pool.isEmpty()) {
                pool = legacy ? cfg.motdLegacy() : cfg.motdModern();
            }
        } else {
            pool = legacy ? cfg.motdLegacy() : cfg.motdModern();
        }

        return filterBlank(pool);
    }

    private boolean hasEndTimer() {
        return maintenance
            .find(MaintenanceType.GLOBAL, "global")
            .filter(state -> state.isCurrentlyActive(System.currentTimeMillis()))
            .map(MaintenanceState::endAt)
            .filter(endAt -> endAt > 0L)
            .isPresent();
    }

    private Map<String, String> pingPlaceholders() {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("scope", "global");
        placeholders.put("reason", maintenance.config().defaultReason());
        placeholders.put("time", "");
        placeholders.put("online", "0");
        placeholders.put("max", "0");

        Optional<MaintenanceState> state = maintenance.blockingState(null);
        if (state.isPresent()) {
            MaintenanceState active = state.get();
            placeholders.put("scope", active.scopeType() == MaintenanceType.GLOBAL ? "global" : active.scopeId());
            placeholders.put("reason", active.reason());
            if (active.endAt() > 0L) {
                long remaining = Math.max(0L, active.endAt() - System.currentTimeMillis());
                placeholders.put("time", DurationParser.format(remaining));
            }
        }

        return placeholders;
    }

    private int legacyMax() {
        return settings.legacyProtocolMax();
    }

    private List<String> linesFor(String key, int protocol) {
        List<String> list = language.rawList(key, protocol);
        if (!list.isEmpty()) {
            return list
                .stream()
                .filter(line -> !line.isBlank())
                .toList();
        }

        String raw = language.raw(key, null, protocol);
        if (raw.equals(key) || raw.equals(LanguageManager.normalizeKey(key)) || raw.isBlank()) return List.of();

        return Arrays.stream(raw.split("\n"))
            .filter(line -> !line.isBlank())
            .toList();
    }

    private static List<String> filterBlank(List<String> pool) {
        if (pool == null || pool.isEmpty()) return List.of();

        return pool
            .stream()
            .filter(line -> line != null && !line.isBlank())
            .toList();
    }
}
