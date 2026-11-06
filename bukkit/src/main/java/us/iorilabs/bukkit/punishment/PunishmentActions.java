package us.iorilabs.bukkit.punishment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import us.iorilabs.api.punishment.PunishmentType;
import us.iorilabs.bukkit.language.CoreLanguage;
import us.iorilabs.bukkit.modules.core.EssentialsPlayers;
import us.iorilabs.proxy.commands.CommandSource;

public final class PunishmentActions {

    private PunishmentActions() {}

    public static ParsedArgs parse(String[] args, boolean requireDuration) {
        if (args == null || args.length == 0) return null;

        String target = args[0];
        boolean silent = false;
        Long durationMillis = null;
        int index = 1;

        if (index < args.length && isSilentFlag(args[index])) {
            silent = true;
            index++;
        }

        if (index < args.length && PunishmentService.looksLikeDuration(args[index])) {
            durationMillis = PunishmentService.parseDurationMillis(args[index]);
            index++;
        } else if (requireDuration) {
            return ParsedArgs.invalidDuration(target);
        }

        if (index < args.length && isSilentFlag(args[index])) {
            silent = true;
            index++;
        }

        String reason = join(args, index);
        return new ParsedArgs(target, reason, durationMillis == null ? 0L : durationMillis, silent, false);
    }

    public static OfflinePlayer resolveTarget(String name) {
        if (name == null || name.trim().isEmpty()) return null;

        Player online = Bukkit.getPlayerExact(name.trim());
        if (online != null) return online;

        return Bukkit.getOfflinePlayer(name.trim());
    }

    public static void apply(
        CoreLanguage language,
        PunishmentService punishments,
        CommandSource source,
        PunishmentType type,
        OfflinePlayer target,
        String inputName,
        String reason,
        long durationMillis,
        boolean silent,
        boolean kickOnly
    ) {
        if (target == null) {
            source.sendMessage(
                language.raw("PUNISHMENTS.PLAYER-NOT-FOUND", Map.of("player", inputName == null ? "?" : inputName))
            );
            return;
        }

        String targetName = target.getName() == null ? (inputName == null ? "unknown" : inputName) : target.getName();
        if (kickOnly && !target.isOnline()) {
            source.sendMessage(language.raw("PUNISHMENTS.PLAYER-OFFLINE", Map.of("player", targetName)));
            return;
        }

        UUID targetId = target.getUniqueId();
        UUID actorId = source.uuidOrNull();
        String actorName = source.name();

        long expiresAt = 0L;
        PunishmentType resolved = type;
        if (durationMillis > 0L) {
            expiresAt = System.currentTimeMillis() + durationMillis;
            if (type == PunishmentType.BAN) resolved = PunishmentType.TEMPBAN;
            if (type == PunishmentType.MUTE) resolved = PunishmentType.TEMPMUTE;
        }

        if (!kickOnly && !ensureNoActive(language, punishments, source, resolved, targetId, targetName)) return;

        Punishment punishment = punishments.punish(
            resolved,
            targetId,
            targetName,
            actorId,
            actorName,
            reason,
            expiresAt,
            silent
        );

        String expiresLabel = expiresLabel(language, expiresAt);
        Map<String, String> placeholders = Map.of(
            "player",
            targetName,
            "actor",
            actorName,
            "reason",
            punishment.reason(),
            "expires",
            expiresLabel,
            "type",
            resolved.name().toLowerCase(Locale.ROOT),
            "id",
            String.valueOf(Math.max(0L, punishment.id()))
        );

        if (resolved == PunishmentType.BAN || resolved == PunishmentType.TEMPBAN || kickOnly) {
            Player online = target.getPlayer();
            if (online != null) {
                String screenKey = kickOnly ? "PUNISHMENTS.KICK.SCREEN" : "PUNISHMENTS.BAN.SCREEN";
                online.kickPlayer(joinLines(language.list(screenKey, placeholders)));
            }
        }

        if (resolved == PunishmentType.MUTE || resolved == PunishmentType.TEMPMUTE) {
            Player online = target.getPlayer();
            if (online != null) sendLines(online, language.list("PUNISHMENTS.MUTE.NOTIFY", placeholders));
        }

        if (resolved == PunishmentType.WARN) {
            Player online = target.getPlayer();
            if (online != null) sendLines(online, language.list("PUNISHMENTS.WARN.NOTIFY", placeholders));
        }

        source.sendMessage(language.raw(successKey(resolved, kickOnly), placeholders));

        if (!silent) {
            List<String> broadcast = language.list("PUNISHMENTS.BROADCAST", placeholders);
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.hasPermission("ioricore.staff") || player.hasPermission("ioricore.admin")) {
                    sendLines(player, broadcast);
                }
            }
        }
    }

    private static boolean ensureNoActive(
        CoreLanguage language,
        PunishmentService punishments,
        CommandSource source,
        PunishmentType type,
        UUID targetId,
        String targetName
    ) {
        if (targetId == null || type == null) return true;

        Optional<Punishment> active = Optional.empty();
        if (type == PunishmentType.BAN || type == PunishmentType.TEMPBAN) {
            active = punishments.activeBan(targetId);
        } else if (type == PunishmentType.MUTE || type == PunishmentType.TEMPMUTE) {
            active = punishments.activeMute(targetId);
        }
        if (active.isEmpty()) return true;

        Punishment existing = active.get();
        source.sendMessage(
            language.raw(
                "PUNISHMENTS.ALREADY-ACTIVE",
                Map.of(
                    "player",
                    targetName == null ? "?" : targetName,
                    "type",
                    existing.type().name().toLowerCase(Locale.ROOT),
                    "id",
                    String.valueOf(Math.max(0L, existing.id())),
                    "expires",
                    expiresLabel(language, existing.expiresAt())
                )
            )
        );
        return false;
    }

    private static void sendLines(Player player, List<String> lines) {
        if (player == null || lines == null || lines.isEmpty()) return;
        for (String line : lines) {
            player.sendMessage(line);
        }
    }

    private static String successKey(PunishmentType type, boolean kickOnly) {
        if (kickOnly) return "PUNISHMENTS.KICK.SUCCESS";
        switch (type) {
            case BAN:
            case TEMPBAN:
                return "PUNISHMENTS.BAN.SUCCESS";
            case MUTE:
            case TEMPMUTE:
                return "PUNISHMENTS.MUTE.SUCCESS";
            case WARN:
                return "PUNISHMENTS.WARN.SUCCESS";
            case KICK:
                return "PUNISHMENTS.KICK.SUCCESS";
            default:
                return "PUNISHMENTS.BAN.SUCCESS";
        }
    }

    private static String expiresLabel(CoreLanguage language, long expiresAt) {
        if (expiresAt <= 0L) return language.raw("PUNISHMENTS.EXPIRES-NEVER");
        long remaining = Math.max(0L, expiresAt - System.currentTimeMillis());
        long seconds = remaining / 1000L;
        if (seconds < 60L) return seconds + "s";
        long minutes = seconds / 60L;
        if (minutes < 60L) return minutes + "m";
        long hours = minutes / 60L;
        if (hours < 48L) return hours + "h";
        long days = hours / 24L;
        return days + "d";
    }

    private static String joinLines(List<String> lines) {
        if (lines == null || lines.isEmpty()) return "";
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) builder.append('\n');
            builder.append(lines.get(i));
        }
        return builder.toString();
    }

    private static String join(String[] args, int from) {
        if (args == null || from >= args.length) return "";
        StringBuilder builder = new StringBuilder();
        for (int i = from; i < args.length; i++) {
            if (i > from) builder.append(' ');
            builder.append(args[i]);
        }
        return builder.toString();
    }

    private static boolean isSilentFlag(String raw) {
        if (raw == null) return false;
        String token = raw.trim().toLowerCase(Locale.ROOT);
        return token.equals("-s") || token.equals("--silent");
    }

    public static void revoke(
        CoreLanguage language,
        PunishmentService punishments,
        CommandSource source,
        PunishmentType primary,
        PunishmentType secondary,
        String usageKey,
        String successKey,
        String input
    ) {
        if (input == null || input.trim().isEmpty()) {
            sendUsage(source, language, usageKey);
            return;
        }

        String token = input.trim();
        Optional<Punishment> punishment = punishments.findForRevoke(token, primary, secondary);
        if (punishment.isEmpty()) {
            if (isNumericId(token)) {
                source.sendMessage(
                    language.raw("PUNISHMENTS.PUNISHMENT-NOT-FOUND", Map.of("id", token, "player", token))
                );
            } else {
                source.sendMessage(
                    language.raw(
                        "PUNISHMENTS.NOT-ACTIVE",
                        Map.of("player", token, "type", primary.name().toLowerCase(Locale.ROOT))
                    )
                );
            }
            return;
        }

        Punishment active = punishment.get();
        if (!punishments.markRevoked(active)) {
            source.sendMessage(
                language.raw(
                    "PUNISHMENTS.NOT-ACTIVE",
                    Map.of(
                        "player",
                        active.targetName() == null || active.targetName().isEmpty() ? token : active.targetName(),
                        "type",
                        primary.name().toLowerCase(Locale.ROOT)
                    )
                )
            );
            return;
        }

        String targetName = active.targetName() == null || active.targetName().isEmpty() ? token : active.targetName();
        Map<String, String> placeholders = Map.of(
            "player",
            targetName,
            "actor",
            source.name(),
            "id",
            String.valueOf(active.id()),
            "type",
            active.type().name().toLowerCase(Locale.ROOT)
        );
        source.sendMessage(language.raw(successKey, placeholders));

        if (primary == PunishmentType.MUTE) {
            Player online = Bukkit.getPlayer(active.targetId());
            if (online != null) sendLines(online, language.list("PUNISHMENTS.UNMUTE.NOTIFY", placeholders));
        }
    }

    private static void sendUsage(CommandSource source, CoreLanguage language, String usageKey) {
        List<String> lines = language.list(usageKey);
        for (String line : lines) {
            source.sendMessage(line);
        }
    }

    private static boolean isNumericId(String raw) {
        if (raw == null || raw.isEmpty()) return false;
        for (int i = 0; i < raw.length(); i++) {
            if (!Character.isDigit(raw.charAt(i))) return false;
        }
        return true;
    }

    public static List<String> suggestPlayers(String prefix) {
        return suggestPlayers(null, prefix);
    }

    public static List<String> suggestPlayers(CommandSource viewer, String prefix) {
        return us.iorilabs.bukkit.modules.core.EssentialsPlayers.suggestOnline(viewer, prefix);
    }

    @Getter
    @Accessors(fluent = true)
    public static final class ParsedArgs {

        private final String target;
        private final String reason;
        private final long durationMillis;
        private final boolean silent;
        private final boolean invalidDuration;

        private ParsedArgs(String target, String reason, long durationMillis, boolean silent, boolean invalidDuration) {
            this.target = target;
            this.reason = reason;
            this.durationMillis = durationMillis;
            this.silent = silent;
            this.invalidDuration = invalidDuration;
        }

        private static ParsedArgs invalidDuration(String target) {
            return new ParsedArgs(target, "", -1L, false, true);
        }
    }
}
