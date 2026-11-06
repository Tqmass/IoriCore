package us.iorilabs.bukkit.modules.staffmode.command;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.language.CoreLanguage;
import us.iorilabs.bukkit.modules.core.EssentialsPlayers;
import us.iorilabs.bukkit.modules.staffmode.StaffChatService;
import us.iorilabs.proxy.commands.CommandParent;
import us.iorilabs.proxy.commands.CommandSource;

@Getter
@Accessors(fluent = true)
public final class StaffChatCommand extends CommandParent {

    private final CoreLanguage language;
    private final StaffChatService staffChat;

    public StaffChatCommand(CoreLanguage language, StaffChatService staffChat) {
        super("staffchat", Arrays.asList("sc"), "ioricore.staffchat.send");
        this.language = language;
        this.staffChat = staffChat;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (
            !source.hasPermission("ioricore.staffchat.send") &&
            !source.hasPermission("ioricore.staffchat.receive") &&
            !source.hasPermission("ioricore.admin")
        ) {
            source.sendMessage(language.raw("GENERAL.NO-PERMISSION"));
            return;
        }

        Player player = EssentialsPlayers.player(source);
        if (player == null) {
            source.sendMessage(language.raw("GENERAL.PLAYER-ONLY"));
            return;
        }

        staffChat.toggle(player);
    }

    @Override
    public List<String> suggest(CommandSource source, String label, String[] args) {
        return Collections.emptyList();
    }

    @Override
    protected void sendUsage(CommandSource source) {
        source.sendMessage(language.raw("STAFFCHAT.TOGGLE-HINT"));
    }

    @Override
    protected String noPermissionMessage() {
        return language.raw("GENERAL.NO-PERMISSION");
    }
}
