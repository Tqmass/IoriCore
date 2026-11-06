package us.iorilabs.bukkit.modules.core.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.modules.core.EssentialsSounds;
import us.iorilabs.bukkit.modules.core.kit.KitDefinition;
import us.iorilabs.bukkit.modules.core.kit.KitService;
import us.iorilabs.proxy.commands.CommandSource;

public final class KitCommand extends EssentialsCommand {

    private final KitService kits;

    public KitCommand(IoriCorePlugin plugin, KitService kits) {
        super(plugin, "kit", Collections.singletonList("kits"), "ioricore.essentials.kit");
        this.kits = kits;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (deny(source, "ioricore.essentials.kit")) return;
        Player player = requirePlayer(source);
        if (player == null) return;

        if (args.length == 0) {
            if (kits.names().isEmpty()) {
                send(source, "ESSENTIALS.KIT.EMPTY");
                return;
            }
            plugin.yamlMenus().open(player, "kits");
            return;
        }

        KitDefinition kit = kits.get(args[0]);
        if (kit == null) {
            send(source, "ESSENTIALS.KIT.NOT-FOUND", Map.of("kit", args[0]));
            return;
        }
        if (
            !source.hasPermission(kit.permission()) &&
            !source.hasPermission("ioricore.kit.*") &&
            !source.hasPermission("ioricore.admin")
        ) {
            send(source, "GENERAL.NO-PERMISSION");
            return;
        }

        long remaining = kits.remainingCooldown(player.getUniqueId(), kit);
        if (remaining > 0L) {
            send(source, "ESSENTIALS.KIT.COOLDOWN", Map.of("kit", kit.name(), "time", formatSeconds(remaining)));
            return;
        }

        if (!kits.give(player, kit)) {
            send(source, "ESSENTIALS.KIT.FAILED", Map.of("kit", kit.name()));
            return;
        }

        EssentialsSounds.play(plugin, player, "SUCCESS");
        send(source, "ESSENTIALS.KIT.SUCCESS", Map.of("kit", kit.name()));
    }

    @Override
    public List<String> suggest(CommandSource source, String label, String[] args) {
        if (args.length != 1) return Collections.emptyList();
        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String name : kits.names()) {
            KitDefinition kit = kits.get(name);
            if (kit == null) continue;
            if (
                !source.hasPermission(kit.permission()) &&
                !source.hasPermission("ioricore.kit.*") &&
                !source.hasPermission("ioricore.admin")
            ) continue;
            if (name.startsWith(prefix)) out.add(name);
        }
        return out;
    }

    @Override
    protected void sendUsage(CommandSource source) {
        sendUsageLines(source, "ESSENTIALS.KIT.USAGE");
    }

    private static String formatSeconds(long millis) {
        long seconds = Math.max(1L, (millis + 999L) / 1000L);
        if (seconds < 60L) return seconds + "s";
        long minutes = seconds / 60L;
        if (minutes < 60L) return minutes + "m";
        return minutes / 60L + "h";
    }
}
