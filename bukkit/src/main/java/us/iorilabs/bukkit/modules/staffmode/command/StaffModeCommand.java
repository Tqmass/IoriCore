package us.iorilabs.bukkit.modules.staffmode.command;

import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.language.CoreLanguage;
import us.iorilabs.bukkit.modules.core.EssentialsPlayers;
import us.iorilabs.bukkit.modules.staffmode.StaffChatService;
import us.iorilabs.bukkit.modules.staffmode.StaffModeService;
import us.iorilabs.bukkit.modules.staffmode.command.argument.StaffModeChatArgument;
import us.iorilabs.bukkit.modules.staffmode.command.argument.StaffModeFreezeArgument;
import us.iorilabs.bukkit.modules.staffmode.command.argument.StaffModeHelpArgument;
import us.iorilabs.proxy.commands.CommandArgument;
import us.iorilabs.proxy.commands.CommandParent;
import us.iorilabs.proxy.commands.CommandSource;

@Getter
@Accessors(fluent = true)
public final class StaffModeCommand extends CommandParent {

    private final CoreLanguage language;
    private final StaffModeService staffMode;
    private final StaffChatService staffChat;

    public StaffModeCommand(CoreLanguage language, StaffModeService staffMode, StaffChatService staffChat) {
        super("staffmode", Arrays.asList("sm", "mod"), "ioricore.staffmode");
        this.language = language;
        this.staffMode = staffMode;
        this.staffChat = staffChat;

        register(new StaffModeFreezeArgument(this));
        register(new StaffModeHelpArgument(this));
        if (staffChat != null) register(new StaffModeChatArgument(this));
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (!source.hasPermission("ioricore.staffmode") && !source.hasPermission("ioricore.admin")) {
            source.sendMessage(language.raw("GENERAL.NO-PERMISSION"));
            return;
        }
        if (args.length == 0) {
            toggle(source);
            return;
        }

        CommandArgument argument = getArgumentsByName().get(args[0].toLowerCase(java.util.Locale.ROOT));
        if (argument == null) {
            sendUsage(source);
            return;
        }

        if (
            argument.permission() != null &&
            !argument.permission().trim().isEmpty() &&
            !source.hasPermission(argument.permission()) &&
            !source.hasPermission("ioricore.admin")
        ) {
            source.sendMessage(noPermissionMessage());
            return;
        }

        argument.execute(source, label, copyArgs(args, 1));
    }

    private static String[] copyArgs(String[] args, int from) {
        if (from >= args.length) return new String[0];
        String[] out = new String[args.length - from];
        System.arraycopy(args, from, out, 0, out.length);
        return out;
    }

    public void toggle(CommandSource source) {
        Player player = EssentialsPlayers.player(source);
        if (player == null) {
            source.sendMessage(language.raw("GENERAL.PLAYER-ONLY"));
            return;
        }
        staffMode.toggle(player);
    }

    public void freeze(CommandSource source, String targetName) {
        Player staff = EssentialsPlayers.player(source);
        if (staff == null) {
            source.sendMessage(language.raw("GENERAL.PLAYER-ONLY"));
            return;
        }
        if (!source.hasPermission("ioricore.staffmode.freeze") && !source.hasPermission("ioricore.admin")) {
            source.sendMessage(language.raw("GENERAL.NO-PERMISSION"));
            return;
        }
        if (targetName == null || targetName.trim().isEmpty()) {
            sendUsage(source);
            return;
        }
        Player target = EssentialsPlayers.findOnline(source, targetName);
        if (target == null) {
            source.sendMessage(language.raw("STAFF-MODE.PLAYER-NOT-FOUND", java.util.Map.of("player", targetName)));
            return;
        }
        staffMode.toggleFreeze(staff, target);
    }

    @Override
    public void sendUsage(CommandSource source) {
        List<String> lines = language.list("STAFF-MODE.USAGE");
        for (String line : lines) {
            source.sendMessage(line);
        }
    }

    @Override
    protected String noPermissionMessage() {
        return language.raw("GENERAL.NO-PERMISSION");
    }
}
