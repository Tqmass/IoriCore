package us.iorilabs.bukkit.modules.core.command;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.modules.core.EssentialsState;
import us.iorilabs.proxy.commands.CommandSource;

public final class RealnameCommand extends EssentialsCommand {

    private final EssentialsState state;

    public RealnameCommand(IoriCorePlugin plugin, EssentialsState state) {
        super(plugin, "realname", Collections.emptyList(), "ioricore.essentials.realname");
        this.state = state;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (deny(source, "ioricore.essentials.realname")) return;
        if (args.length == 0) {
            sendUsage(source);
            return;
        }

        String query = ChatColor.stripColor(
            ChatColor.translateAlternateColorCodes('&', String.join(" ", args))
        ).toLowerCase(Locale.ROOT);

        for (Player online : Bukkit.getOnlinePlayers()) {
            String nick = state.nick(online.getUniqueId());
            if (nick == null) continue;
            String plain = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', nick)).toLowerCase(
                Locale.ROOT
            );
            if (plain.equals(query) || plain.contains(query)) {
                send(source, "ESSENTIALS.REALNAME.SUCCESS", Map.of("nick", nick, "player", online.getName()));
                return;
            }
        }

        send(source, "ESSENTIALS.REALNAME.NOT-FOUND", Map.of("nick", args[0]));
    }

    @Override
    public java.util.List<String> suggest(CommandSource source, String label, String[] args) {
        return Collections.emptyList();
    }

    @Override
    protected void sendUsage(CommandSource source) {
        sendUsageLines(source, "ESSENTIALS.REALNAME.USAGE");
    }
}
