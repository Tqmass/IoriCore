package us.iorilabs.proxy.bungee.impl.listener;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import net.kyori.adventure.platform.bungeecord.BungeeAudiences;
import us.iorilabs.proxy.IoriProxy;
import us.iorilabs.proxy.bungee.player.BungeeProxyPlayer;
import us.iorilabs.proxy.staffreg.StaffRegService;

public final class BungeeStaffRegListener implements Listener {

    private final IoriProxy core;
    private final ProxyServer proxy;
    private final BungeeAudiences audiences;

    public BungeeStaffRegListener(IoriProxy core, ProxyServer proxy, BungeeAudiences audiences) {
        this.core = core;
        this.proxy = proxy;
        this.audiences = audiences;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onServerConnect(ServerConnectEvent event) {
        StaffRegService staffReg = core.staffReg();
        if (staffReg == null) return;
        if (!staffReg.allowServerSwitch(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onChat(ChatEvent event) {
        StaffRegService staffReg = core.staffReg();
        if (staffReg == null) return;
        if (!(event.getSender() instanceof ProxiedPlayer player)) return;
        if (!event.getMessage().startsWith("/")) return;

        String command = event.getMessage().substring(1);
        BungeeProxyPlayer wrapped = new BungeeProxyPlayer(proxy, player, audiences);
        if (!staffReg.allowCommand(wrapped, command)) {
            event.setCancelled(true);
            staffReg.onCommandDenied(wrapped, !staffReg.isRestricted(player.getUniqueId()));
        }
    }
}
