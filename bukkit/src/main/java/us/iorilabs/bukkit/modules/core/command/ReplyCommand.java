package us.iorilabs.bukkit.modules.core.command;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.commands.BukkitCommandSource;
import us.iorilabs.bukkit.modules.chatformat.ChatText;
import us.iorilabs.bukkit.modules.core.EssentialsState;
import us.iorilabs.bukkit.modules.core.ignore.IgnoreService;
import us.iorilabs.bukkit.punishment.PunishmentsModule;
import us.iorilabs.proxy.commands.CommandSource;

public final class ReplyCommand extends EssentialsCommand {

    private final EssentialsState state;
    private final IgnoreService ignore;

    public ReplyCommand(IoriCorePlugin plugin, EssentialsState state, IgnoreService ignore) {
        super(plugin, "reply", Collections.singletonList("r"), "ioricore.essentials.reply");
        this.state = state;
        this.ignore = ignore;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (deny(source, "ioricore.essentials.reply")) return;
        Player player = requirePlayer(source);
        if (player == null) return;
        if (PunishmentsModule.blockIfMuted(plugin, player)) return;
        if (args.length == 0) {
            sendUsage(source);
            return;
        }

        UUID targetId = state.replyTarget(player.getUniqueId());
        if (targetId == null) {
            send(source, "ESSENTIALS.REPLY.NONE");
            return;
        }

        Player target = Bukkit.getPlayer(targetId);
        if (target == null) {
            send(source, "ESSENTIALS.REPLY.OFFLINE");
            return;
        }

        if (ignore.ignores(target.getUniqueId(), player.getUniqueId()) && !source.hasPermission("ioricore.admin")) {
            send(source, "ESSENTIALS.IGNORE.BLOCKED", Map.of("player", target.getName()));
            return;
        }

        String message = String.join(" ", args).trim();
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
        sendUsageLines(source, "ESSENTIALS.REPLY.USAGE");
    }

    @Override
    public java.util.List<String> suggest(CommandSource source, String label, String[] args) {
        return Collections.emptyList();
    }
}
