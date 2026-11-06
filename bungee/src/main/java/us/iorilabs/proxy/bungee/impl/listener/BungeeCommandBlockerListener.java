package us.iorilabs.proxy.bungee.impl.listener;

import java.util.Map;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import us.iorilabs.proxy.IoriProxy;
import us.iorilabs.proxy.bungee.BungeePlatform;
import us.iorilabs.proxy.bungee.command.BungeeCommandSource;
import us.iorilabs.proxy.commands.CommandBlockerService;
import us.iorilabs.proxy.player.ProxyPlayer;

public final class BungeeCommandBlockerListener implements Listener {

    private final IoriProxy core;
    private final BungeePlatform platform;

    public BungeeCommandBlockerListener(IoriProxy core, BungeePlatform platform) {
        this.core = core;
        this.platform = platform;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onChat(ChatEvent event) {
        if (!(event.getSender() instanceof ProxiedPlayer player)) return;
        if (!event.getMessage().startsWith("/")) return;
        if (core.commandBlocker() == null || !core.configs().settings().moduleEnabled("command_whitelist")) return;

        BungeeCommandSource source = new BungeeCommandSource(player, platform.audiences());
        if (!core.commandBlocker().isBlocked(source, event.getMessage())) return;

        event.setCancelled(true);
        source.sendMessage(core.language().raw("general.command_blocked"));

        String notifyPerm = core.configs().commands().permission("commandwhitelist-notify");
        if (notifyPerm == null || notifyPerm.isBlank()) return;

        String root = CommandBlockerService.normalizeRoot(event.getMessage());
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
