package us.iorilabs.bukkit.modules.tab.command;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.language.CoreLanguage;
import us.iorilabs.bukkit.modules.tab.TabService;
import us.iorilabs.proxy.commands.CommandParent;
import us.iorilabs.proxy.commands.CommandSource;

public final class TabCommand extends CommandParent {

    private final CoreLanguage language;
    private final TabService service;

    public TabCommand(IoriCorePlugin plugin, TabService service) {
        super("tab", Collections.emptyList(), "ioricore.tab");
        this.language = plugin.language();
        this.service = service;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (args.length >= 1 && "reload".equalsIgnoreCase(args[0])) {
            if (!source.hasPermission("ioricore.admin")) {
                source.sendMessage(language.raw("GENERAL.NO-PERMISSION"));
                return;
            }
            service.reload();
            source.sendMessage(language.raw("TAB.RELOADED"));
            return;
        }
        sendUsage(source);
    }

    @Override
    public List<String> suggest(CommandSource source, String label, String[] args) {
        if (args.length == 1) return filter(Collections.singletonList("reload"), args[0]);
        return Collections.emptyList();
    }

    @Override
    protected String noPermissionMessage() {
        return language.raw("GENERAL.NO-PERMISSION");
    }

    @Override
    protected void sendUsage(CommandSource source) {
        for (String line : language.list("TAB.USAGE")) {
            source.sendMessage(line);
        }
    }

    private static List<String> filter(List<String> options, String token) {
        String needle = token == null ? "" : token.toLowerCase(Locale.ROOT);
        return options
            .stream()
            .filter(o -> o.toLowerCase(Locale.ROOT).startsWith(needle))
            .toList();
    }
}
