package us.iorilabs.bukkit.modules.core.command;

import java.util.Collections;
import java.util.Map;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.modules.core.EssentialsState;
import us.iorilabs.proxy.commands.CommandSource;

public final class NickCommand extends EssentialsCommand {

    private final EssentialsState state;

    public NickCommand(IoriCorePlugin plugin, EssentialsState state) {
        super(plugin, "nick", Collections.singletonList("nickname"), "ioricore.essentials.nick");
        this.state = state;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (deny(source, "ioricore.essentials.nick")) return;
        Player player = requirePlayer(source);
        if (player == null) return;
        if (args.length == 0) {
            sendUsage(source);
            return;
        }

        if ("off".equalsIgnoreCase(args[0]) || "reset".equalsIgnoreCase(args[0])) {
            state.setNick(player.getUniqueId(), null);
            send(source, "ESSENTIALS.NICK.RESET");
            return;
        }

        String nick = String.join(" ", args);
        if (nick.length() > 32) {
            send(source, "ESSENTIALS.NICK.INVALID");
            return;
        }
        state.setNick(player.getUniqueId(), nick);
        send(source, "ESSENTIALS.NICK.SUCCESS", Map.of("nick", nick));
    }

    @Override
    protected void sendUsage(CommandSource source) {
        sendUsageLines(source, "ESSENTIALS.NICK.USAGE");
    }

    @Override
    public java.util.List<String> suggest(CommandSource source, String label, String[] args) {
        return Collections.emptyList();
    }
}
