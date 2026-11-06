package us.iorilabs.bukkit.modules.core.command;

import java.util.Arrays;
import java.util.Map;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.commands.BukkitCommandSource;
import us.iorilabs.bukkit.modules.chatformat.ChatText;
import us.iorilabs.bukkit.modules.core.EssentialsPlayers;
import us.iorilabs.bukkit.modules.core.EssentialsState;
import us.iorilabs.bukkit.modules.core.ignore.IgnoreService;
import us.iorilabs.bukkit.punishment.PunishmentsModule;
import us.iorilabs.proxy.commands.CommandSource;

public final class MsgCommand extends EssentialsCommand {

    private final EssentialsState state;
    private final IgnoreService ignore;

    public MsgCommand(IoriCorePlugin plugin, EssentialsState state, IgnoreService ignore) {
        super(plugin, "msg", Arrays.asList("tell", "w", "whisper", "message"), "ioricore.essentials.msg");
        this.state = state;
        this.ignore = ignore;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (deny(source, "ioricore.essentials.msg")) return;
        Player player = requirePlayer(source);
        if (player == null) return;
        if (PunishmentsModule.blockIfMuted(plugin, player)) return;
        if (args.length < 2) {
            sendUsage(source);
            return;
        }

        Player target = EssentialsPlayers.findOnline(source, args[0]);
        if (target == null) {
            send(source, "ESSENTIALS.PLAYER-NOT-FOUND", Map.of("player", args[0]));
            return;
        }

        if (ignore.ignores(target.getUniqueId(), player.getUniqueId()) && !source.hasPermission("ioricore.admin")) {
            send(source, "ESSENTIALS.IGNORE.BLOCKED", Map.of("player", target.getName()));
            return;
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (i > 1) builder.append(' ');
            builder.append(args[i]);
        }
        String message = builder.toString().trim();
        if (message.isEmpty()) {
            sendUsage(source);
            return;
        }

        String text = ChatText.sanitize(player, message);
        state.setReply(player.getUniqueId(), target.getUniqueId());
        player.sendMessage(render(player, "ESSENTIALS.MSG.TO", target.getName(), text));
        new BukkitCommandSource(target).sendMessage(render(target, "ESSENTIALS.MSG.FROM", player.getName(), text));
    }

    private String render(Player receiver, String key, String player, String text) {
        return ChatText.inject(language.raw(receiver, key, Map.of("player", player, "message", ChatText.SLOT)), text);
    }

    @Override
    protected void sendUsage(CommandSource source) {
        sendUsageLines(source, "ESSENTIALS.MSG.USAGE");
    }
}
