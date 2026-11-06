package us.iorilabs.bukkit.modules.core.command;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.modules.core.EssentialsSounds;
import us.iorilabs.proxy.commands.CommandSource;

public final class SkullCommand extends EssentialsCommand {

    public SkullCommand(IoriCorePlugin plugin) {
        super(plugin, "skull", Collections.singletonList("head"), "ioricore.essentials.skull");
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (deny(source, "ioricore.essentials.skull")) return;
        Player player = requirePlayer(source);
        if (player == null) return;

        String owner = args.length == 0 ? player.getName() : args[0].trim();
        if (owner.isEmpty() || owner.length() > 16 || !owner.matches("[A-Za-z0-9_]+")) {
            send(source, "ESSENTIALS.SKULL.INVALID", Map.of("player", owner));
            return;
        }

        ItemStack skull = createSkull(owner);
        if (skull == null) {
            send(source, "ESSENTIALS.SKULL.FAILED");
            return;
        }

        Map<Integer, ItemStack> leftover = player.getInventory().addItem(skull);
        for (ItemStack drop : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }
        EssentialsSounds.play(plugin, player, "SUCCESS");
        send(source, "ESSENTIALS.SKULL.SUCCESS", Map.of("player", owner));
    }

    @SuppressWarnings("deprecation")
    private static ItemStack createSkull(String owner) {
        Material material = Material.matchMaterial("SKULL_ITEM");
        if (material == null) material = Material.matchMaterial("PLAYER_HEAD");
        if (material == null) return null;

        ItemStack stack;
        if ("SKULL_ITEM".equals(material.name())) {
            stack = new ItemStack(material, 1, (short) 3);
        } else {
            stack = new ItemStack(material, 1);
        }

        if (!(stack.getItemMeta() instanceof SkullMeta)) return stack;
        SkullMeta meta = (SkullMeta) stack.getItemMeta();
        meta.setOwner(owner);
        meta.setDisplayName(owner + "'s Head");
        stack.setItemMeta(meta);
        return stack;
    }

    @Override
    public java.util.List<String> suggest(CommandSource source, String label, String[] args) {
        if (args.length <= 1) {
            return us.iorilabs.bukkit.modules.core.EssentialsPlayers.suggestOnline(
                args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT)
            );
        }
        return Collections.emptyList();
    }

    @Override
    protected void sendUsage(CommandSource source) {
        sendUsageLines(source, "ESSENTIALS.SKULL.USAGE");
    }
}
