package us.iorilabs.proxy.bungee.impl.listener;

import java.util.ArrayList;
import java.util.List;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.TabCompleteEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import us.iorilabs.proxy.IoriProxy;
import us.iorilabs.proxy.bungee.BungeePlatform;
import us.iorilabs.proxy.bungee.command.BungeeCommandSource;
import us.iorilabs.proxy.commands.CommandBlockerService;

public final class BungeeTabCompleteListener implements Listener {

    private final IoriProxy core;
    private final BungeePlatform platform;

    public BungeeTabCompleteListener(IoriProxy core, BungeePlatform platform) {
        this.core = core;
        this.platform = platform;
    }

    @EventHandler
    public void onTabComplete(TabCompleteEvent event) {
        if (core.tabCompleteFilter() == null || core.commandBlocker() == null) return;
        if (!core.configs().settings().moduleEnabled("command_whitelist")) return;
        if (!(event.getSender() instanceof ProxiedPlayer player)) return;

        BungeeCommandSource source = new BungeeCommandSource(player, platform.audiences());
        String cursor = event.getCursor() == null ? "" : event.getCursor();
        String root = CommandBlockerService.normalizeRoot(cursor);

        if (!root.isBlank() && core.commandBlocker().isBlocked(source, root)) {
            event.getSuggestions().clear();
            return;
        }

        List<String> suggestions = new ArrayList<>(event.getSuggestions());
        List<String> filtered = core.tabCompleteFilter().filter(source, cursor, suggestions);
        event.getSuggestions().clear();
        event.getSuggestions().addAll(filtered);
    }
}
