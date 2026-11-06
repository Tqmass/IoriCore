package us.iorilabs.bukkit.modules.core.command;

import java.util.Collections;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.commands.BukkitCommandSource;
import us.iorilabs.bukkit.modules.chatformat.ChatText;
import us.iorilabs.bukkit.punishment.PunishmentsModule;
import us.iorilabs.proxy.commands.CommandSource;

public final class MeCommand extends EssentialsCommand {

    public MeCommand(IoriCorePlugin plugin) {
        super(plugin, "me", Collections.singletonList("action"), "ioricore.essentials.me");
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (deny(source, "ioricore.essentials.me")) return;
        Player player = requirePlayer(source);
        if (player == null) return;
        if (PunishmentsModule.blockIfMuted(plugin, player)) return;
        if (args.length == 0) {
            sendUsage(source);
            return;
        }

        String message = String.join(" ", args).trim();
        if (message.isEmpty() || message.length() > 200) {
            send(source, "ESSENTIALS.ME.TOO-LONG");
            return;
        }

        String text = ChatText.sanitize(player, message);
        Map<String, String> placeholders = Map.of("player", player.getName(), "message", ChatText.SLOT);
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendMessage(ChatText.inject(language.raw(online, "ESSENTIALS.ME.FORMAT", placeholders), text));
        }
    }

    @Override
    public java.util.List<String> suggest(CommandSource source, String label, String[] args) {
        return Collections.emptyList();
    }

    @Override
    protected void sendUsage(CommandSource source) {
        sendUsageLines(source, "ESSENTIALS.ME.USAGE");
    }
}
