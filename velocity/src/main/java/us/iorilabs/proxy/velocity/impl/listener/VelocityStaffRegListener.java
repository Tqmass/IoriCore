package us.iorilabs.proxy.velocity.impl.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import us.iorilabs.proxy.IoriProxy;
import us.iorilabs.proxy.commands.CommandBlockerService;
import us.iorilabs.proxy.staffreg.StaffRegService;
import us.iorilabs.proxy.velocity.player.VelocityProxyPlayer;

public final class VelocityStaffRegListener {

    private final IoriProxy core;
    private final ProxyServer proxy;

    public VelocityStaffRegListener(IoriProxy core, ProxyServer proxy) {
        this.core = core;
        this.proxy = proxy;
    }

    @Subscribe
    public void onPreConnect(ServerPreConnectEvent event) {
        StaffRegService staffReg = core.staffReg();
        if (staffReg == null) return;
        if (!staffReg.allowServerSwitch(event.getPlayer().getUniqueId())) {
            event.setResult(ServerPreConnectEvent.ServerResult.denied());
        }
    }

    @Subscribe
    public void onCommand(CommandExecuteEvent event) {
        StaffRegService staffReg = core.staffReg();
        if (staffReg == null) return;
        if (!(event.getCommandSource() instanceof Player player)) return;

        VelocityProxyPlayer wrapped = new VelocityProxyPlayer(proxy, player);
        if (!staffReg.allowCommand(wrapped, event.getCommand())) {
            event.setResult(CommandExecuteEvent.CommandResult.denied());
            staffReg.onCommandDenied(wrapped, !staffReg.isRestricted(player.getUniqueId()));
        }
    }
}
