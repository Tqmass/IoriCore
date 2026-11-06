package us.iorilabs.bukkit.punishment;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.language.CoreLanguage;

@RequiredArgsConstructor
public final class BanListener implements Listener {

    private final IoriCorePlugin plugin;
    private final PunishmentService punishments;

    @EventHandler(priority = EventPriority.HIGH)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) return;
        if (punishments == null) return;

        Optional<Punishment> ban = punishments.activeBan(event.getUniqueId());
        if (ban.isEmpty()) return;

        Punishment punishment = ban.get();
        CoreLanguage language = plugin.language();
        Map<String, String> placeholders = Map.of(
            "player",
            event.getName() == null ? "" : event.getName(),
            "actor",
            punishment.actorName() == null ? "Console" : punishment.actorName(),
            "reason",
            punishments.sanitizeReason(punishment.reason()),
            "expires",
            expiresLabel(language, punishment.expiresAt()),
            "type",
            punishment.type() == null ? "ban" : punishment.type().name().toLowerCase(Locale.ROOT)
        );

        event.disallow(
            AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
            joinLines(language.list("PUNISHMENTS.BAN.SCREEN", placeholders))
        );
    }

    private static String expiresLabel(CoreLanguage language, long expiresAt) {
        if (expiresAt <= 0L) {
            String never = language.raw("PUNISHMENTS.EXPIRES-NEVER");
            return never == null || never.isEmpty() || never.startsWith("PUNISHMENTS.") ? "permanent" : never;
        }
        long remaining = Math.max(0L, expiresAt - System.currentTimeMillis());
        long seconds = remaining / 1000L;
        if (seconds < 60L) return seconds + "s";
        long minutes = seconds / 60L;
        if (minutes < 60L) return minutes + "m";
        long hours = minutes / 60L;
        if (hours < 48L) return hours + "h";
        return hours / 24L + "d";
    }

    private static String joinLines(List<String> lines) {
        if (lines == null || lines.isEmpty()) return "Banned";
        StringJoiner joiner = new StringJoiner("\n");
        for (String line : lines) {
            if (line != null) joiner.add(line);
        }
        return joiner.toString();
    }
}
