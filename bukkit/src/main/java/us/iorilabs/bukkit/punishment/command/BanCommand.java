package us.iorilabs.bukkit.punishment.command;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.OfflinePlayer;
import us.iorilabs.api.punishment.PunishmentType;
import us.iorilabs.bukkit.language.CoreLanguage;
import us.iorilabs.bukkit.punishment.PunishmentActions;
import us.iorilabs.bukkit.punishment.PunishmentService;
import us.iorilabs.proxy.commands.CommandParent;
import us.iorilabs.proxy.commands.CommandSource;

@Getter
@Accessors(fluent = true)
public final class BanCommand extends CommandParent {

    private final CoreLanguage language;
    private final PunishmentService punishments;

    public BanCommand(CoreLanguage language, PunishmentService punishments) {
        super("ban", Collections.singletonList("tempban"), "ioricore.punish.ban");
        this.language = language;
        this.punishments = punishments;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (!source.hasPermission("ioricore.punish.ban") && !source.hasPermission("ioricore.admin")) {
            source.sendMessage(language.raw("GENERAL.NO-PERMISSION"));
            return;
        }
        if (args.length == 0) {
            sendUsage(source);
            return;
        }

        boolean requireDuration = label != null && label.equalsIgnoreCase("tempban");
        PunishmentActions.ParsedArgs parsed = PunishmentActions.parse(args, requireDuration);
        if (parsed == null) {
            sendUsage(source);
            return;
        }
        if (parsed.invalidDuration()) {
            source.sendMessage(language.raw("PUNISHMENTS.DURATION-INVALID", Map.of("duration", "")));
            sendUsage(source);
            return;
        }

        OfflinePlayer target = PunishmentActions.resolveTarget(parsed.target());
        if (target == null || target.getUniqueId() == null) {
            source.sendMessage(language.raw("PUNISHMENTS.PLAYER-NOT-FOUND", Map.of("player", parsed.target())));
            return;
        }

        PunishmentActions.apply(
            language,
            punishments,
            source,
            PunishmentType.BAN,
            target,
            parsed.target(),
            parsed.reason(),
            parsed.durationMillis(),
            parsed.silent(),
            false
        );
    }

    @Override
    public List<String> suggest(CommandSource source, String label, String[] args) {
        if (args.length <= 1) {
            String prefix = args.length == 0 ? "" : args[0];
            return PunishmentActions.suggestPlayers(source, prefix);
        }
        if (args.length == 2) {
            String token = args[1].toLowerCase(Locale.ROOT);
            List<String> out = new java.util.ArrayList<>();
            if ("-s".startsWith(token)) out.add("-s");
            if ("1h".startsWith(token)) out.add("1h");
            if ("1d".startsWith(token)) out.add("1d");
            if ("7d".startsWith(token)) out.add("7d");
            return out;
        }
        return Collections.emptyList();
    }

    @Override
    protected void sendUsage(CommandSource source) {
        List<String> lines = language.list("PUNISHMENTS.BAN.USAGE");
        for (String line : lines) {
            source.sendMessage(line);
        }
    }

    @Override
    protected String noPermissionMessage() {
        return language.raw("GENERAL.NO-PERMISSION");
    }
}
