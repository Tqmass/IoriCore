package us.iorilabs.proxy.commands;

import java.util.List;
import java.util.UUID;
import us.iorilabs.proxy.IoriProxy;
import us.iorilabs.proxy.player.ProxyPlayer;
import us.iorilabs.proxy.staffchat.StaffChatService;

public final class StaffChatCommand implements ProxyCommand {

    private final IoriProxy proxy;
    private final StaffChatService staffChat;

    public StaffChatCommand(IoriProxy proxy, StaffChatService staffChat) {
        this.proxy = proxy;
        this.staffChat = staffChat;
    }

    @Override
    public String name() {
        return "staffchat";
    }

    @Override
    public List<String> aliases() {
        return List.of("sc");
    }

    @Override
    public String permission() {
        return proxy.configs().commands().permission("staffchat_send");
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (!source.isPlayer()) {
            source.sendMessage(proxy.language().raw("general.player_only"));
            return;
        }

        UUID uuid = source.uuidOrNull();
        if (uuid == null) return;

        if (args.length == 0) {
            boolean on = staffChat.toggle(uuid);
            source.sendMessage(proxy.language().raw(on ? "staffchat.toggle_on" : "staffchat.toggle_off"));
            return;
        }

        ProxyPlayer player = proxy.platform().findPlayerByUuid(uuid).orElse(null);
        if (player == null) return;

        staffChat.send(player, String.join(" ", args));
    }
}
