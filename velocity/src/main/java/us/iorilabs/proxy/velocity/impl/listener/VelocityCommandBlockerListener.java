package us.iorilabs.proxy.velocity.impl.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import java.util.Map;
import us.iorilabs.proxy.IoriProxy;
import us.iorilabs.proxy.commands.CommandBlockerService;
import us.iorilabs.proxy.player.ProxyPlayer;
import us.iorilabs.proxy.velocity.command.VelocityCommandSource;

public final class VelocityCommandBlockerListener {

    private final IoriProxy core;

    public VelocityCommandBlockerListener(IoriProxy core) {
        this.core = core;
    }

    @Subscribe
    public void onCommand(CommandExecuteEvent event) {
        if (core.commandBlocker() == null || !core.configs().settings().moduleEnabled("command_whitelist")) return;

        VelocityCommandSource source = new VelocityCommandSource(event.getCommandSource());
        if (!core.commandBlocker().isBlocked(source, event.getCommand())) return;

        event.setResult(CommandExecuteEvent.CommandResult.denied());
        source.sendMessage(core.language().raw("general.command_blocked"));

        String notifyPerm = core.configs().commands().permission("commandwhitelist-notify");
        if (notifyPerm == null || notifyPerm.isBlank()) return;

        String root = CommandBlockerService.normalizeRoot(event.getCommand());
        String line = core
            .language()
            .raw("alerts.command_blocked", Map.of("player", source.name(), "command", "/" + root));
        for (ProxyPlayer online : core.platform().onlinePlayers()) {
            if (online.hasPermission(notifyPerm)) {
                online.sendMessage(line);
            }
        }
    }
}
