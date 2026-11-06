package us.iorilabs.proxy.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
public final class MaintenanceConfig {

    private static final List<Integer> DEFAULT_INTERVALS = List.of(60, 30, 15, 10, 5, 4, 3, 2, 1);

    @Getter
    private final YamlConfig yaml;

    public MaintenanceConfig(YamlConfig yaml) {
        this.yaml = yaml;
    }

    public String defaultReason() {
        return yaml.getString("default_reason", "Maintenance in progress");
    }

    public boolean kickNonWhitelisted() {
        return yaml.getBoolean("kick_non_whitelisted", true);
    }

    public String bypassPermission() {
        return yaml.getString("bypass_permission", "ioriproxy.maintenance.bypass");
    }

    public boolean motdOverride() {
        return yaml.getBoolean("motd.override", true);
    }

    public boolean motdShowEndTimer() {
        return yaml.getBoolean("motd.show_end_timer", true);
    }

    public List<String> motdModern() {
        return yaml.getStringList("motd.modern");
    }

    public List<String> motdLegacy() {
        return yaml.getStringList("motd.legacy");
    }

    public List<String> motdTimerModern() {
        return yaml.getStringList("motd.timer_modern");
    }

    public List<String> motdTimerLegacy() {
        return yaml.getStringList("motd.timer_legacy");
    }

    public boolean playerListEnabled() {
        return yaml.getBoolean("player_list.enabled", true);
    }

    public String playerListCountMessage() {
        return yaml.getString("player_list.count_message", "").stripTrailing();
    }

    public String playerListCountTimerMessage() {
        return yaml.getString("player_list.count_timer_message", "").stripTrailing();
    }

    public List<String> playerListHover() {
        return yaml.getStringList("player_list.hover");
    }

    public List<String> playerListHoverTimer() {
        return yaml.getStringList("player_list.hover_timer");
    }

    public List<String> playerListSamplePlayers() {
        return yaml.getStringList("player_list.sample_players");
    }

    public String kickDescription() {
        return yaml.getString("kick.description", "").stripTrailing();
    }

    public String kickDescriptionLegacy() {
        String legacy = yaml.getString("kick.description-legacy", "").stripTrailing();
        if (legacy == null || legacy.isBlank()) return kickDescription();

        return legacy;
    }

    public boolean kickRedirectLobby() {
        return yaml.getBoolean("kick.redirect_lobby", true);
    }

    public List<String> allowDuringMaintenance() {
        return yaml.getStringList("allow_during_maintenance");
    }

    public boolean countdownEnabled() {
        return yaml.getBoolean("countdown.enabled", true);
    }

    public List<Integer> countdownIntervals() {
        Map<String, Object> countdown = yaml.getSection("countdown");
        if (countdown.isEmpty()) return DEFAULT_INTERVALS;

        Object intervals = countdown.get("INTERVALS-SECONDS");
        if (!(intervals instanceof List<?> list)) return DEFAULT_INTERVALS;

        List<Integer> out = new ArrayList<>();

        for (Object value : list) {
            if (value instanceof Number number) {
                out.add(number.intValue());
            }
        }

        return out;
    }

    public boolean actionbar() {
        return yaml.getBoolean("countdown.actionbar", true);
    }

    public boolean title() {
        return yaml.getBoolean("countdown.title", true);
    }

    public String countdownChat() {
        return yaml.getString("countdown.chat", "").stripTrailing();
    }

    public String countdownTitleMain() {
        return yaml.getString("countdown.title_main", "").stripTrailing();
    }

    public String countdownTitleSub() {
        return yaml.getString("countdown.title_sub", "").stripTrailing();
    }

    public String countdownStarted() {
        return yaml.getString("countdown.started", "").stripTrailing();
    }

    public String countdownEnded() {
        return yaml.getString("countdown.ended", "").stripTrailing();
    }

    public List<String> whitelist() {
        return yaml.getStringList("whitelist");
    }

    public Map<String, List<String>> modes() {
        Map<String, Object> section = yaml.getSection("modes");
        Map<String, List<String>> modes = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : section.entrySet()) {
            if (entry.getValue() instanceof List<?> list) {
                modes.put(entry.getKey().toLowerCase(Locale.ROOT), list.stream().map(String::valueOf).toList());
            }
        }

        return modes;
    }

    public List<String> serversForMode(String mode) {
        return modes().getOrDefault(mode.toLowerCase(Locale.ROOT), Collections.emptyList());
    }
}
