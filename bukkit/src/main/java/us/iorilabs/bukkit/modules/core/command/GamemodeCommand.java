package us.iorilabs.bukkit.modules.core.command;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.commands.BukkitCommandSource;
import us.iorilabs.bukkit.modules.core.EssentialsPlayers;
import us.iorilabs.bukkit.modules.core.EssentialsSounds;
import us.iorilabs.proxy.commands.CommandSource;

public final class GamemodeCommand extends EssentialsCommand {

    public GamemodeCommand(IoriCorePlugin plugin) {
        super(plugin, "gamemode", Arrays.asList("gm", "gms", "gmc", "gma", "gmsp"), "ioricore.essentials.gamemode");
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (deny(source, "ioricore.essentials.gamemode")) return;

        String normalized = label == null ? "gamemode" : label.toLowerCase(Locale.ROOT);
        GameMode mode = modeFromAlias(normalized);
        String[] rest = args;
        if (mode == null) {
            if (args.length == 0) {
                sendUsage(source);
                return;
            }
            mode = parseMode(args[0]);
            if (mode == null) {
                send(source, "ESSENTIALS.GAMEMODE.INVALID", Map.of("mode", args[0]));
                return;
            }
            rest = Arrays.copyOfRange(args, 1, args.length);
        }

        Player self = EssentialsPlayers.player(source);
        Player target;
        if (rest.length == 0) {
            target = requirePlayer(source);
            if (target == null) return;
        } else {
            if (deny(source, "ioricore.essentials.gamemode.others")) return;
            target = EssentialsPlayers.findOnline(source, rest[0]);
            if (target == null) {
                send(source, "ESSENTIALS.PLAYER-NOT-FOUND", Map.of("player", rest[0]));
                return;
            }
        }

        target.setGameMode(mode);
        EssentialsSounds.play(plugin, target, "SUCCESS");
        send(
            source,
            "ESSENTIALS.GAMEMODE.SUCCESS",
            Map.of("player", target.getName(), "mode", mode.name().toLowerCase(Locale.ROOT))
        );
        if (self == null || !self.getUniqueId().equals(target.getUniqueId())) {
            new BukkitCommandSource(target).sendMessage(
                language.raw(target, "ESSENTIALS.GAMEMODE.TARGET", Map.of("mode", mode.name().toLowerCase(Locale.ROOT)))
            );
        }
    }

    private static GameMode modeFromAlias(String label) {
        switch (label) {
            case "gms":
                return GameMode.SURVIVAL;
            case "gmc":
                return GameMode.CREATIVE;
            case "gma":
                return GameMode.ADVENTURE;
            case "gmsp":
                return GameMode.SPECTATOR;
            default:
                return null;
        }
    }

    private static GameMode parseMode(String raw) {
        if (raw == null) return null;
        String value = raw.trim().toLowerCase(Locale.ROOT);
        switch (value) {
            case "0":
            case "s":
            case "survival":
                return GameMode.SURVIVAL;
            case "1":
            case "c":
            case "creative":
                return GameMode.CREATIVE;
            case "2":
            case "a":
            case "adventure":
                return GameMode.ADVENTURE;
            case "3":
            case "sp":
            case "spectator":
                return GameMode.SPECTATOR;
            default:
                return null;
        }
    }

    @Override
    protected void sendUsage(CommandSource source) {
        sendUsageLines(source, "ESSENTIALS.GAMEMODE.USAGE");
    }

    @Override
    public List<String> suggest(CommandSource source, String label, String[] args) {
        String normalized = label == null ? "gamemode" : label.toLowerCase(Locale.ROOT);
        if (modeFromAlias(normalized) != null) {
            if (args.length <= 1) return EssentialsPlayers.suggestOnline(source, args.length == 0 ? "" : args[0]);
            return Collections.emptyList();
        }
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            List<String> modes = Arrays.asList("survival", "creative", "adventure", "spectator", "0", "1", "2", "3");
            List<String> out = new java.util.ArrayList<>();
            for (String mode : modes) {
                if (mode.startsWith(prefix)) out.add(mode);
            }
            return out;
        }
        if (args.length == 2) return EssentialsPlayers.suggestOnline(source, args[1]);
        return Collections.emptyList();
    }
}
