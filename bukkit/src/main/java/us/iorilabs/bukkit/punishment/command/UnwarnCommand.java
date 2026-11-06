package us.iorilabs.bukkit.punishment.command;

import java.util.Collections;
import java.util.List;
import lombok.Getter;
import lombok.experimental.Accessors;
import us.iorilabs.api.punishment.PunishmentType;
import us.iorilabs.bukkit.language.CoreLanguage;
import us.iorilabs.bukkit.punishment.PunishmentActions;
import us.iorilabs.bukkit.punishment.PunishmentService;
import us.iorilabs.proxy.commands.CommandParent;
import us.iorilabs.proxy.commands.CommandSource;

@Getter
@Accessors(fluent = true)
public final class UnwarnCommand extends CommandParent {

    private final CoreLanguage language;
    private final PunishmentService punishments;

    public UnwarnCommand(CoreLanguage language, PunishmentService punishments) {
        super("unwarn", Collections.emptyList(), "ioricore.punish.unwarn");
        this.language = language;
        this.punishments = punishments;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (!source.hasPermission("ioricore.punish.unwarn") && !source.hasPermission("ioricore.admin")) {
            source.sendMessage(language.raw("GENERAL.NO-PERMISSION"));
            return;
        }
        if (args.length == 0) {
            sendUsage(source);
            return;
        }

        PunishmentActions.revoke(
            language,
            punishments,
            source,
            PunishmentType.WARN,
            PunishmentType.WARN,
            "PUNISHMENTS.UNWARN.USAGE",
            "PUNISHMENTS.UNWARN.SUCCESS",
            args[0]
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
        List<String> lines = language.list("PUNISHMENTS.UNWARN.USAGE");
        for (String line : lines) {
            source.sendMessage(line);
        }
    }

    @Override
    protected String noPermissionMessage() {
        return language.raw("GENERAL.NO-PERMISSION");
    }
}
