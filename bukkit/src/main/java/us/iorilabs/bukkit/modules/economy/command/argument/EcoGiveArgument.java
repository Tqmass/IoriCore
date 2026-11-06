package us.iorilabs.bukkit.modules.economy.command.argument;

import java.util.List;
import java.util.Map;
import org.bukkit.OfflinePlayer;
import us.iorilabs.bukkit.commands.BukkitCommandSource;
import us.iorilabs.bukkit.modules.core.EssentialsPlayers;
import us.iorilabs.bukkit.modules.economy.command.EcoCommand;
import us.iorilabs.proxy.commands.CommandArgument;
import us.iorilabs.proxy.commands.CommandSource;

public final class EcoGiveArgument extends CommandArgument {

    private final EcoCommand parent;

    public EcoGiveArgument(EcoCommand parent) {
        super("give", "ioricore.economy.eco");
        this.parent = parent;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (args.length < 2) {
            parent.sendUsage(source);
            return;
        }

        OfflinePlayer target = parent.resolveTarget(args[0]);
        if (target == null || target.getUniqueId() == null) {
            parent.send(source, "ECONOMY.PLAYER-NOT-FOUND", Map.of("player", args[0]));
            return;
        }

        Double amount = parent.parseAmount(args[1]);
        if (amount == null || amount <= 0.0D) {
            parent.send(source, "ECONOMY.INVALID-AMOUNT");
            return;
        }

        if (!parent.economy().deposit(target.getUniqueId(), amount)) {
            parent.send(source, "ECONOMY.ECO-FAILED");
            return;
        }

        String formatted = parent.economy().format(amount);
        parent.send(source, "ECONOMY.ECO-GIVE", Map.of("player", parent.nameOf(target), "amount", formatted));
        if (target.isOnline() && target.getPlayer() != null) {
            new BukkitCommandSource(target.getPlayer()).sendMessage(
                parent.language().raw(target.getPlayer(), "ECONOMY.ECO-GIVE-TARGET", Map.of("amount", formatted))
            );
        }
    }

    @Override
    public List<String> suggest(CommandSource source, String label, String[] args) {
        if (args.length <= 1) return EssentialsPlayers.suggestOnline(source, args.length == 0 ? "" : args[0]);
        return java.util.Collections.emptyList();
    }
}
