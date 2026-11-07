package us.iorilabs.proxy.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import us.iorilabs.proxy.IoriProxy;
import us.iorilabs.proxy.commands.argument.LambdaCommandArgument;
import us.iorilabs.proxy.utilities.ColorUtil;

public final class RootCommand extends CommandParent {

    private static final String HELP_FRAME_BAR = "&7&m------------------------------------------&r";

    private final IoriProxy proxy;

    public RootCommand(IoriProxy proxy) {
        super(
            proxy.configs().commands().rootName(),
            proxy.configs().commands().rootAliases(),
            proxy.configs().commands().permission("reload")
        );
        this.proxy = proxy;
        register(
            LambdaCommandArgument.of("reload", (source, args) -> {
                proxy.reload();
                source.sendMessage(proxy.language().raw("general.reload_success"));
            })
        );
        register(
            LambdaCommandArgument.of("info", (source, args) ->
                source.sendMessage(
                    "<gray>" +
                        IoriProxy.NAME +
                        " <white>" +
                        proxy.platform().version() +
                        "</white> on <white>" +
                        proxy.platform().platformName() +
                        "</white> <dark_gray>[proxy: " +
                        proxy.configs().settings().proxyId() +
                        "]</dark_gray></gray>"
                )
            )
        );
        register(
            LambdaCommandArgument.of("help", (source, args) -> {
                int page = 1;
                if (args.length >= 1) {
                    try {
                        page = Integer.parseInt(args[0]);
                    } catch (NumberFormatException ignored) {
                        page = 1;
                    }
                }
                sendHelp(source, page);
            })
        );
        setDefaultArgument(LambdaCommandArgument.of("default", (source, args) -> sendHelp(source, 1)));
    }

    @Override
    protected void sendUsage(CommandSource source) {
        sendHelp(source, 1);
    }

    private void sendHelp(CommandSource source, int requestedPage) {
        List<List<String>> pages = loadHelpPages();
        if (pages.isEmpty()) {
            for (String line : proxy.language().rawList("general.help")) {
                source.sendMessage(line);
            }
            return;
        }

        int total = pages.size();
        int page = Math.max(1, Math.min(requestedPage, total));
        String bar = HELP_FRAME_BAR;
        String title = proxy.language().raw("general.help_title");
        if (title.equals("general.help_title")) {
            title = "&b&lProxy &8» &7Comandos";
        }
        List<String> lines = stripHelpPageBody(pages.get(page - 1), bar, title);
        source.sendMessage(bar);
        source.sendMessage(title);
        for (String line : lines) {
            source.sendMessage(line == null || line.isBlank() ? " " : line);
        }
        source.sendMessage(navBar(page, total));
        source.sendMessage(bar);
    }

    private static String normalizeHelpToken(String value) {
        if (value == null) return "";
        return value.replace('§', '&').trim();
    }

    private static boolean helpLineMatches(String line, String expected) {
        if (line == null || expected == null) return false;
        return normalizeHelpToken(line).equals(normalizeHelpToken(expected));
    }

    private static boolean isBlankHelpLine(String line) {
        return line == null || line.isBlank();
    }

    private List<String> stripHelpPageBody(List<String> page, String bar, String title) {
        if (page == null || page.isEmpty()) return List.of();
        ArrayList<String> out = new ArrayList<>(page);
        while (!out.isEmpty() && helpLineMatches(out.get(0), bar)) {
            out.remove(0);
        }
        while (!out.isEmpty() && helpLineMatches(out.get(out.size() - 1), bar)) {
            out.remove(out.size() - 1);
        }
        while (!out.isEmpty() && isBlankHelpLine(out.get(0))) {
            out.remove(0);
        }
        while (!out.isEmpty() && isBlankHelpLine(out.get(out.size() - 1))) {
            out.remove(out.size() - 1);
        }
        if (!out.isEmpty() && helpLineMatches(out.get(0), title)) {
            out.remove(0);
            while (!out.isEmpty() && isBlankHelpLine(out.get(0))) {
                out.remove(0);
            }
        }
        return List.copyOf(out);
    }

    private List<List<String>> loadHelpPages() {
        List<List<String>> pages = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            List<String> page = proxy.language().rawList("general.help.pages." + i);
            if (page == null || page.isEmpty()) page = proxy.language().rawList("general.help.page-" + i);
            if (page == null || page.isEmpty()) page = proxy.language().rawList("general.help.page_" + i);
            if (page == null || page.isEmpty()) break;
            pages.add(page);
        }

        if (!pages.isEmpty()) return pages;

        List<String> legacy = proxy.language().rawList("general.help");
        if (legacy == null || legacy.isEmpty()) return List.of();

        int size = 12;
        for (int i = 0; i < legacy.size(); i += size) {
            pages.add(legacy.subList(i, Math.min(i + size, legacy.size())));
        }
        return pages;
    }

    private Component navBar(int page, int total) {
        String root = proxy.configs().commands().rootName();
        String prevHover = proxy.language().raw("general.help_prev_hover");
        String nextHover = proxy.language().raw("general.help_next_hover");
        if (prevHover.equals("general.help_prev_hover")) prevHover = "Previous page";
        if (nextHover.equals("general.help_next_hover")) nextHover = "Next page";
        String lang = proxy.language().language().toLowerCase(Locale.ROOT);
        if ("es".equals(lang)) {
            if ("Previous page".equals(prevHover)) prevHover = "Página anterior";
            if ("Next page".equals(nextHover)) nextHover = "Siguiente página";
        }

        String label = proxy
            .language()
            .raw("general.help_page_label", Map.of("page", String.valueOf(page), "pages", String.valueOf(total)));
        if (label.equals("general.help_page_label")) {
            label = "&7Página &f" + page + "&7/&f" + total;
        }

        Component prev;
        if (page > 1) {
            prev = Component.text("[◀]", NamedTextColor.AQUA)
                .clickEvent(ClickEvent.runCommand("/" + root + " help " + (page - 1)))
                .hoverEvent(HoverEvent.showText(Component.text(prevHover, NamedTextColor.GRAY)));
        } else {
            prev = Component.text("[◀]", NamedTextColor.DARK_GRAY);
        }

        Component next;
        if (page < total) {
            next = Component.text("[▶]", NamedTextColor.AQUA)
                .clickEvent(ClickEvent.runCommand("/" + root + " help " + (page + 1)))
                .hoverEvent(HoverEvent.showText(Component.text(nextHover, NamedTextColor.GRAY)));
        } else {
            next = Component.text("[▶]", NamedTextColor.DARK_GRAY);
        }

        return Component.empty()
            .append(prev)
            .append(Component.text(" "))
            .append(ColorUtil.parse(label))
            .append(Component.text(" "))
            .append(next);
    }

    @Override
    protected String noPermissionMessage() {
        return proxy.language().raw("general.no_permission");
    }
}
