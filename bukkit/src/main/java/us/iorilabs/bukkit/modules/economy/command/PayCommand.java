package us.iorilabs.bukkit.modules.economy.command;

import java.util.Collections;
import java.util.Map;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.commands.BukkitCommandSource;
import us.iorilabs.bukkit.modules.core.EssentialsPlayers;
import us.iorilabs.bukkit.modules.economy.EconomyService;
import us.iorilabs.proxy.commands.CommandSource;

public final class PayCommand extends EconomyCommandBase {

    public PayCommand(IoriCorePlugin plugin, EconomyService economy) {
        super(plugin, economy, "pay", Collections.emptyList(), "ioricore.economy.pay");
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (deny(source, "ioricore.economy.pay")) return;

        Player self = requirePlayer(source);
        if (self == null) return;

        if (args.length < 2) {
            sendUsage(source);
            return;
        }

        Player target = EssentialsPlayers.findOnline(source, args[0]);
        if (target == null) {
            send(source, "ECONOMY.PLAYER-NOT-FOUND", Map.of("player", args[0]));
            return;
        }
        if (target.getUniqueId().equals(self.getUniqueId())) {
            send(source, "ECONOMY.PAY-SELF");
            return;
        }

        Double amount = parseAmount(args[1]);
        if (amount == null || amount <= 0.0D) {
            send(source, "ECONOMY.INVALID-AMOUNT");
            return;
        }

        if (!economy.has(self.getUniqueId(), amount)) {
            send(
                source,
                "ECONOMY.INSUFFICIENT",
                Map.of("balance", economy.format(economy.balance(self.getUniqueId())))
            );
            return;
        }

        if (!economy.pay(self.getUniqueId(), target.getUniqueId(), amount)) {
            send(source, "ECONOMY.PAY-FAILED");
            return;
        }

        String formatted = economy.format(amount);
        send(source, "ECONOMY.PAY-SENT", Map.of("player", target.getName(), "amount", formatted));
        new BukkitCommandSource(target).sendMessage(
            language.raw(target, "ECONOMY.PAY-RECEIVED", Map.of("player", self.getName(), "amount", formatted))
        );
    }

    @Override
    protected void sendUsage(CommandSource source) {
        sendUsageLines(source, "ECONOMY.PAY-USAGE");
    }
}
