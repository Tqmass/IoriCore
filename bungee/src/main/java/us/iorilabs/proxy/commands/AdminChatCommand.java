package us.iorilabs.proxy.commands;

import java.util.List;
import java.util.UUID;
import us.iorilabs.proxy.IoriProxy;
import us.iorilabs.proxy.adminchat.AdminChatService;
import us.iorilabs.proxy.player.ProxyPlayer;

public final class AdminChatCommand implements ProxyCommand {

    private final IoriProxy proxy;
    private final AdminChatService adminChat;

    public AdminChatCommand(IoriProxy proxy, AdminChatService adminChat) {
        this.proxy = proxy;
        this.adminChat = adminChat;
    }

    @Override
    public String name() {
        return "adminchat";
    }

    @Override
    public List<String> aliases() {
        return List.of("ac");
    }

    @Override
    public String permission() {
        // Open: players request via /ac; admins use adminchat.send inside.
        return "";
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (!source.isPlayer()) {
            source.sendMessage(proxy.language().raw("general.player_only"));
            return;
        }

        UUID uuid = source.uuidOrNull();
        if (uuid == null) return;

        ProxyPlayer player = proxy.platform().findPlayerByUuid(uuid).orElse(null);
        if (player == null) return;

        boolean canAdmin = player.hasPermission(proxy.configs().commands().permission("adminchat_send"));

        if (args.length == 0) {
            if (!canAdmin) {
                source.sendMessage(proxy.language().raw("adminchat.usage"));
                return;
            }

            boolean on = adminChat.toggle(uuid);
            source.sendMessage(proxy.language().raw(on ? "adminchat.toggle_on" : "adminchat.toggle_off"));
            return;
        }

        String message = String.join(" ", args);
        if (canAdmin) {
            adminChat.sendAdmin(player, message);
            return;
        }

        // Jugadores / staff sin AC send → alerta de solicitud a request.receive
        if (!player.hasPermission(proxy.configs().commands().permission("request_send"))) {
            source.sendMessage(proxy.language().raw("general.no_permission"));
            return;
        }

        adminChat.sendRequestAlert(player, message);
        source.sendMessage(proxy.language().raw("adminchat.request_sent"));
    }
}
