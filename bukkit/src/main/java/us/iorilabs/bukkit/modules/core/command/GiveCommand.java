package us.iorilabs.bukkit.modules.core.command;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.modules.core.EssentialsPlayers;
import us.iorilabs.bukkit.modules.core.EssentialsSounds;
import us.iorilabs.proxy.commands.CommandSource;

public final class GiveCommand extends EssentialsCommand {

    public GiveCommand(IoriCorePlugin plugin) {
        super(plugin, "give", Arrays.asList("i", "item"), "ioricore.essentials.give");
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (deny(source, "ioricore.essentials.give")) return;

        boolean selfAlias = "i".equalsIgnoreCase(label) || "item".equalsIgnoreCase(label);
        Player target;
        String materialName;
        String amountRaw;

        if (selfAlias) {
            target = requirePlayer(source);
            if (target == null) return;
            if (args.length < 1) {
                sendUsage(source);
                return;
            }
            materialName = args[0];
            amountRaw = args.length >= 2 ? args[1] : "1";
        } else if (args.length >= 2) {
            target = EssentialsPlayers.findOnline(source, args[0]);
            if (target == null) {
                send(source, "ESSENTIALS.PLAYER-NOT-FOUND", Map.of("player", args[0]));
                return;
            }
            materialName = args[1];
            amountRaw = args.length >= 3 ? args[2] : "1";
        } else if (args.length == 1) {
            target = requirePlayer(source);
            if (target == null) return;
            materialName = args[0];
            amountRaw = "1";
        } else {
            sendUsage(source);
            return;
        }

        Material material = matchMaterial(materialName);
        if (material == null || material == Material.AIR) {
            send(source, "ESSENTIALS.GIVE.INVALID", Map.of("item", materialName));
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(amountRaw);
        } catch (NumberFormatException ex) {
            send(source, "ESSENTIALS.GIVE.INVALID-AMOUNT", Map.of("amount", amountRaw));
            return;
        }
        if (amount < 1 || amount > 2304) {
            send(source, "ESSENTIALS.GIVE.INVALID-AMOUNT", Map.of("amount", amountRaw));
            return;
        }

        target.getInventory().addItem(new ItemStack(material, amount));
        EssentialsSounds.play(plugin, target, "SUCCESS");
        send(
            source,
            "ESSENTIALS.GIVE.SUCCESS",
            Map.of("player", target.getName(), "item", material.name(), "amount", String.valueOf(amount))
        );
    }

    private static Material matchMaterial(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        String name = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        Material matched = Material.matchMaterial(name);
        if (matched != null) return matched;
        try {
            return Material.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    @Override
    protected void sendUsage(CommandSource source) {
        sendUsageLines(source, "ESSENTIALS.GIVE.USAGE");
    }
}
