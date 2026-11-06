package us.iorilabs.proxy.bungee.impl.listener;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import us.iorilabs.proxy.IoriProxy;
import us.iorilabs.proxy.adminchat.AdminChatService;
import us.iorilabs.proxy.staffchat.StaffChatService;

public final class BungeeStaffChatListener implements Listener {

    private final IoriProxy core;

    public BungeeStaffChatListener(IoriProxy core) {
        this.core = core;
    }

    @EventHandler
    public void onChat(ChatEvent event) {
        if (!(event.getSender() instanceof ProxiedPlayer handle)) return;

        if (
            !core.configs().settings().moduleEnabled("staffchat") &&
            !core.configs().settings().moduleEnabled("adminchat")
        ) {
            return;
        }

        var playerOpt = core.platform().findPlayerByUuid(handle.getUniqueId());
        if (playerOpt.isEmpty()) return;

        var player = playerOpt.get();
        String message = event.getMessage();
        if (message.startsWith("/")) return;

        AdminChatService adminChat = core.adminChat();
        if (adminChat != null && core.configs().settings().moduleEnabled("adminchat")) {
            if (adminChat.interceptChat(player, message)) {
                event.setCancelled(true);
                return;
            }
        }

        StaffChatService staffChat = core.staffChat();
        if (staffChat != null && core.configs().settings().moduleEnabled("staffchat")) {
            if (staffChat.interceptChat(player, message)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerDisconnectEvent event) {
        if (core.staffChat() != null) {
            core.staffChat().untoggle(event.getPlayer().getUniqueId());
        }

        if (core.adminChat() != null) {
            core.adminChat().untoggle(event.getPlayer().getUniqueId());
        }
    }
}
