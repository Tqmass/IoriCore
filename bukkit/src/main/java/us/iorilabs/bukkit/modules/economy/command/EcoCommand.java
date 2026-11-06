package us.iorilabs.bukkit.modules.economy.command;

import java.util.Arrays;
import java.util.Map;
import org.bukkit.OfflinePlayer;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.modules.economy.EconomyService;
import us.iorilabs.bukkit.modules.economy.command.argument.EcoGiveArgument;
import us.iorilabs.bukkit.modules.economy.command.argument.EcoHelpArgument;
import us.iorilabs.bukkit.modules.economy.command.argument.EcoSetArgument;
import us.iorilabs.bukkit.modules.economy.command.argument.EcoTakeArgument;
import us.iorilabs.proxy.commands.CommandSource;

public final class EcoCommand extends EconomyCommandBase {

    public EcoCommand(IoriCorePlugin plugin, EconomyService economy) {
        super(plugin, economy, "eco", Arrays.asList("economy"), "ioricore.economy.eco");
        register(new EcoGiveArgument(this));
        register(new EcoTakeArgument(this));
        register(new EcoSetArgument(this));
        register(new EcoHelpArgument(this));
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (deny(source, "ioricore.economy.eco")) return;
        super.execute(source, label, args);
    }

    public void send(CommandSource source, String key, Map<String, String> placeholders) {
        super.send(source, key, placeholders);
    }

    public void send(CommandSource source, String key) {
        super.send(source, key);
    }

    public OfflinePlayer resolveTarget(String name) {
        return super.resolveTarget(name);
    }

    public Double parseAmount(String raw) {
        return super.parseAmount(raw);
    }

    public String nameOf(OfflinePlayer player) {
        return super.nameOf(player);
    }

    @Override
    public void sendUsage(CommandSource source) {
        sendUsageLines(source, "ECONOMY.ECO-USAGE");
    }
}
