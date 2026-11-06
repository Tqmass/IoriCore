package us.iorilabs.bukkit.modules.staffauth.command;

import java.util.Collections;
import java.util.Map;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.commands.BukkitCommandSource;
import us.iorilabs.bukkit.language.CoreLanguage;
import us.iorilabs.bukkit.modules.staffauth.StaffRegService;
import us.iorilabs.bukkit.modules.staffauth.command.argument.StaffRegCreateArgument;
import us.iorilabs.bukkit.modules.staffauth.command.argument.StaffRegLinkArgument;
import us.iorilabs.bukkit.modules.staffauth.command.argument.StaffRegStatusArgument;
import us.iorilabs.proxy.commands.CommandParent;
import us.iorilabs.proxy.commands.CommandSource;

public final class StaffRegCommand extends CommandParent {

    private final CoreLanguage language;
    private final StaffRegService service;

    public StaffRegCommand(CoreLanguage language, StaffRegService service) {
        super("staffreg", Collections.emptyList(), "ioricore.staffreg");
        this.language = language;
        this.service = service;
        register(new StaffRegCreateArgument(this, service));
        register(new StaffRegLinkArgument(this, service));
        register(new StaffRegStatusArgument(this, service));
    }

    public StaffRegService service() {
        return service;
    }

    public void send(CommandSource source, String key) {
        source.sendMessage(language.raw(source, key));
    }

    public void send(CommandSource source, String key, Map<String, String> placeholders) {
        source.sendMessage(language.raw(source, key, placeholders));
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (args.length > 0) {
            super.execute(source, label, args);
            return;
        }
        if (!(source instanceof BukkitCommandSource)) {
            send(source, "GENERAL.PLAYER-ONLY");
            return;
        }
        Player player = ((BukkitCommandSource) source).playerOrNull();
        if (player == null) {
            send(source, "GENERAL.PLAYER-ONLY");
            return;
        }
        service.beginDiscordLink(player);
    }

    @Override
    protected void sendUsage(CommandSource source) {
        send(source, "STAFFREG.USAGE");
    }

    @Override
    protected String noPermissionMessage() {
        return language.raw("GENERAL.NO-PERMISSION");
    }
}
