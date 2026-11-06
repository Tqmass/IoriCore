package us.iorilabs.bukkit.modules.economy.command;

import java.util.Arrays;
import java.util.Map;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.modules.economy.EconomyService;
import us.iorilabs.proxy.commands.CommandSource;

public final class BalanceCommand extends EconomyCommandBase {

    public BalanceCommand(IoriCorePlugin plugin, EconomyService economy) {
        super(plugin, economy, "balance", Arrays.asList("money", "bal"), "ioricore.economy.balance");
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (deny(source, "ioricore.economy.balance")) return;

        OfflinePlayer target;
        if (args.length == 0) {
            Player self = requirePlayer(source);
            if (self == null) return;
            target = self;
        } else {
            if (deny(source, "ioricore.economy.balance.others")) return;
            target = resolveTarget(args[0]);
            if (target == null || target.getUniqueId() == null) {
                send(source, "ECONOMY.PLAYER-NOT-FOUND", Map.of("player", args[0]));
                return;
            }
        }

        double balance = economy.balance(target.getUniqueId());
        send(source, "ECONOMY.BALANCE", Map.of("player", nameOf(target), "balance", economy.format(balance)));
    }

    @Override
    protected void sendUsage(CommandSource source) {
        sendUsageLines(source, "ECONOMY.BALANCE-USAGE");
    }
}
