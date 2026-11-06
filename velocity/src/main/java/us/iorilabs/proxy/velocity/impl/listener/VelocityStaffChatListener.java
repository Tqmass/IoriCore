package us.iorilabs.proxy.velocity.impl.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.Player;
import us.iorilabs.proxy.IoriProxy;
import us.iorilabs.proxy.adminchat.AdminChatService;
import us.iorilabs.proxy.staffchat.StaffChatService;

public final class VelocityStaffChatListener {

    private final IoriProxy core;

    public VelocityStaffChatListener(IoriProxy core) {
        this.core = core;
    }

    @Subscribe
    public void onChat(PlayerChatEvent event) {
        if (
            !core.configs().settings().moduleEnabled("staffchat") &&
            !core.configs().settings().moduleEnabled("adminchat")
        ) {
            return;
        }

        Player handle = event.getPlayer();
        var playerOpt = core.platform().findPlayerByUuid(handle.getUniqueId());
        if (playerOpt.isEmpty()) return;

        var player = playerOpt.get();
        AdminChatService adminChat = core.adminChat();
        if (adminChat != null && core.configs().settings().moduleEnabled("adminchat")) {
            if (adminChat.interceptChat(player, event.getMessage())) {
                event.setResult(PlayerChatEvent.ChatResult.denied());
                return;
            }
        }

        StaffChatService staffChat = core.staffChat();
        if (staffChat != null && core.configs().settings().moduleEnabled("staffchat")) {
            if (staffChat.interceptChat(player, event.getMessage())) {
                event.setResult(PlayerChatEvent.ChatResult.denied());
            }
        }
    }

    @Subscribe
    public void onQuit(DisconnectEvent event) {
        if (core.staffChat() != null) {
            core.staffChat().untoggle(event.getPlayer().getUniqueId());
        }

        if (core.adminChat() != null) {
            core.adminChat().untoggle(event.getPlayer().getUniqueId());
        }
    }
}
