package us.iorilabs.bukkit.modules.scoreboard.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.commands.BukkitCommandSource;
import us.iorilabs.bukkit.language.CoreLanguage;
import us.iorilabs.bukkit.modules.scoreboard.ScoreboardService;
import us.iorilabs.proxy.commands.CommandParent;
import us.iorilabs.proxy.commands.CommandSource;

public final class ScoreboardCommand extends CommandParent {

    private final CoreLanguage language;
    private final ScoreboardService service;

    public ScoreboardCommand(IoriCorePlugin plugin, ScoreboardService service) {
        super("scoreboard", Arrays.asList("sb", "sidebar"), "ioricore.scoreboard");
        this.language = plugin.language();
        this.service = service;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (!source.hasPermission("ioricore.scoreboard") && !source.hasPermission("ioricore.admin")) {
            source.sendMessage(language.raw("GENERAL.NO-PERMISSION"));
            return;
        }
        if (!(source instanceof BukkitCommandSource)) {
            source.sendMessage(language.raw("GENERAL.PLAYER-ONLY"));
            return;
        }
        Player player = ((BukkitCommandSource) source).playerOrNull();
        if (player == null) {
            source.sendMessage(language.raw("GENERAL.PLAYER-ONLY"));
            return;
        }

        if (args.length >= 1 && "reload".equalsIgnoreCase(args[0])) {
            if (!source.hasPermission("ioricore.admin")) {
                source.sendMessage(language.raw("GENERAL.NO-PERMISSION"));
                return;
            }
            service.reload();
            source.sendMessage(language.raw("SCOREBOARD.RELOADED"));
            return;
        }

        if (args.length >= 1 && "style".equalsIgnoreCase(args[0])) {
            if (args.length < 2) {
                source.sendMessage(
                    language.raw(
                        "SCOREBOARD.STYLE-USAGE",
                        Collections.singletonMap("styles", String.join(", ", service.styles()))
                    )
                );
                return;
            }
            if (!service.setStyle(player, args[1])) {
                source.sendMessage(
                    language.raw(
                        "SCOREBOARD.STYLE-UNKNOWN",
                        Collections.singletonMap("styles", String.join(", ", service.styles()))
                    )
                );
                return;
            }
            source.sendMessage(
                language.raw("SCOREBOARD.STYLE-SET", Collections.singletonMap("style", service.styleOf(player)))
            );
            return;
        }

        boolean enabled = service.toggle(player);
        source.sendMessage(language.raw(enabled ? "SCOREBOARD.TOGGLE-ON" : "SCOREBOARD.TOGGLE-OFF"));
    }

    @Override
    public List<String> suggest(CommandSource source, String label, String[] args) {
        if (args.length == 1) return filter(Arrays.asList("style", "reload"), args[0]);
        if (args.length == 2 && "style".equalsIgnoreCase(args[0])) return filter(service.styles(), args[1]);
        return Collections.emptyList();
    }

    @Override
    protected String noPermissionMessage() {
        return language.raw("GENERAL.NO-PERMISSION");
    }

    @Override
    protected void sendUsage(CommandSource source) {
        for (String line : language.list("SCOREBOARD.USAGE")) {
            source.sendMessage(line);
        }
    }

    private static List<String> filter(List<String> options, String token) {
        String needle = token == null ? "" : token.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(needle)) out.add(option);
        }
        return out;
    }
}
