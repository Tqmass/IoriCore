package us.iorilabs.bukkit.punishment.command;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.entity.Player;
import us.iorilabs.api.modules.ModuleId;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.language.CoreLanguage;
import us.iorilabs.bukkit.modules.CoreModule;
import us.iorilabs.bukkit.modules.core.EssentialsPlayers;
import us.iorilabs.bukkit.punishment.PunishmentActions;
import us.iorilabs.bukkit.punishment.PunishmentsModule;
import us.iorilabs.proxy.commands.CommandParent;
import us.iorilabs.proxy.commands.CommandSource;

@Getter
@Accessors(fluent = true)
public final class PunishCommand extends CommandParent {

    private final IoriCorePlugin plugin;
    private final CoreLanguage language;

    public PunishCommand(IoriCorePlugin plugin) {
        super("punish", Collections.emptyList(), "ioricore.punish.menu");
        this.plugin = plugin;
        this.language = plugin.language();
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (!canUse(source)) {
            source.sendMessage(language.raw("GENERAL.NO-PERMISSION"));
            return;
        }
        if (args.length == 0) {
            sendUsage(source);
            return;
        }

        Player staff = EssentialsPlayers.player(source);
        if (staff == null) {
            source.sendMessage(language.raw("GENERAL.PLAYER-ONLY"));
            return;
        }

        String target = args[0].trim();
        if (PunishmentActions.resolveTarget(target) == null) {
            source.sendMessage(language.raw("PUNISH.PLAYER-NOT-FOUND", Map.of("player", target)));
            return;
        }

        plugin.yamlMenus().open(staff, menuMain(), Map.of("target", target, "player", target));
    }

    private String menuMain() {
        if (plugin.modules() == null) return "punish";
        CoreModule module = plugin.modules().get(ModuleId.PUNISHMENTS);
        if (!(module instanceof PunishmentsModule punishments) || punishments.config() == null) return "punish";
        return punishments.config().menuMain();
    }

    @Override
    public List<String> suggest(CommandSource source, String label, String[] args) {
        if (args.length <= 1) {
            String prefix = args.length == 0 ? "" : args[0];
            return PunishmentActions.suggestPlayers(source, prefix);
        }
        return Collections.emptyList();
    }

    private boolean canUse(CommandSource source) {
        return (
            source.hasPermission("ioricore.punish.menu") ||
            source.hasPermission("ioricore.punish.ban") ||
            source.hasPermission("ioricore.punish.mute") ||
            source.hasPermission("ioricore.punish.warn") ||
            source.hasPermission("ioricore.punish.kick") ||
            source.hasPermission("ioricore.admin")
        );
    }

    @Override
    protected void sendUsage(CommandSource source) {
        for (String line : language.list("PUNISH.USAGE")) {
            source.sendMessage(line);
        }
    }

    @Override
    protected String noPermissionMessage() {
        return language.raw("GENERAL.NO-PERMISSION");
    }
}
