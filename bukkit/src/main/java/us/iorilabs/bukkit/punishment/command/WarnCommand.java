package us.iorilabs.bukkit.punishment.command;

import java.util.Collections;
import java.util.List;
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
public final class WarnCommand extends CommandParent {

    private final CoreLanguage language;
    private final PunishmentService punishments;

    public WarnCommand(CoreLanguage language, PunishmentService punishments) {
        super("warn", Collections.emptyList(), "ioricore.punish.warn");
        this.language = language;
        this.punishments = punishments;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (!source.hasPermission("ioricore.punish.warn") && !source.hasPermission("ioricore.admin")) {
            source.sendMessage(language.raw("GENERAL.NO-PERMISSION"));
            return;
        }
        if (args.length == 0) {
            sendUsage(source);
            return;
        }

        PunishmentActions.ParsedArgs parsed = PunishmentActions.parse(args, false);
        if (parsed == null) {
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
            PunishmentType.WARN,
            target,
            parsed.target(),
            parsed.reason(),
            0L,
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
        return Collections.emptyList();
    }

    @Override
    protected void sendUsage(CommandSource source) {
        List<String> lines = language.list("PUNISHMENTS.WARN.USAGE");
        for (String line : lines) {
            source.sendMessage(line);
        }
    }

    @Override
    protected String noPermissionMessage() {
        return language.raw("GENERAL.NO-PERMISSION");
    }
}
