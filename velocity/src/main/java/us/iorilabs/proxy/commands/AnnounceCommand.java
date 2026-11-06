package us.iorilabs.proxy.commands;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import us.iorilabs.proxy.IoriProxy;
import us.iorilabs.proxy.player.ProxyPlayer;
import us.iorilabs.proxy.utilities.TextNames;

public final class AnnounceCommand implements ProxyCommand {

    private final IoriProxy proxy;

    public AnnounceCommand(IoriProxy proxy) {
        this.proxy = proxy;
    }

    @Override
    public String name() {
        return "announce";
    }

    @Override
    public List<String> aliases() {
        return List.of("announcer", "titleannounce");
    }

    @Override
    public String permission() {
        return proxy.configs().commands().permission("announce");
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (args.length < 1) {
            source.sendMessage(proxy.language().raw("announce.usage"));
            return;
        }

        String target = "all";
        String rest;
        if (isTargetToken(args[0])) {
            target = args[0];
            if (args.length < 2) {
                source.sendMessage(proxy.language().raw("announce.usage"));
                return;
            }
            rest = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).trim();
        } else {
            rest = String.join(" ", args).trim();
        }

        String titleBody;
        String subtitleBody = "";
        int pipe = rest.indexOf('|');
        if (pipe >= 0) {
            titleBody = rest.substring(0, pipe).trim();
            subtitleBody = rest.substring(pipe + 1).trim();
        } else {
            titleBody = rest;
        }

        if (titleBody.isBlank()) {
            source.sendMessage(proxy.language().raw("announce.usage"));
            return;
        }

        // Strip user color codes — fixed aesthetic.
        titleBody = stripColors(titleBody);
        subtitleBody = stripColors(subtitleBody);

        boolean all = target.equalsIgnoreCase("all") || target.equalsIgnoreCase("*");
        int sent = 0;
        int legacyMax = proxy.language().legacyProtocolMax();

        for (ProxyPlayer player : proxy.platform().onlinePlayers()) {
            if (!all) {
                String server = player.currentServer();
                if (server == null || !server.equalsIgnoreCase(target)) continue;
            }

            boolean legacy = player.protocolVersion() <= legacyMax;
            String title = proxy
                .language()
                .raw(legacy ? "announce.title_legacy" : "announce.title_modern", Map.of("text", titleBody));
            String sub = subtitleBody.isBlank()
                ? ""
                : proxy
                      .language()
                      .raw(
                          legacy ? "announce.subtitle_legacy" : "announce.subtitle_modern",
                          Map.of("text", subtitleBody)
                      );
            proxy.platform().sendTitle(player.uuid(), title, sub);
            playAnnounceSound(player, legacy);
            sent++;
        }

        source.sendMessage(
            proxy
                .language()
                .raw(
                    "announce.sent",
                    Map.of("count", String.valueOf(sent), "target", all ? "all" : TextNames.displayServer(target))
                )
        );
    }

    private void playAnnounceSound(ProxyPlayer player, boolean legacy) {
        String sound = legacy ? "note.pling" : "block.note_block.pling";
        proxy.platform().playSound(player.uuid(), sound, 1.0f, 1.0f);
    }

    private boolean isTargetToken(String token) {
        if (token == null) return false;
        if (token.equalsIgnoreCase("all") || token.equals("*")) return true;

        return proxy.platform().server(token).isPresent();
    }

    private static String stripColors(String input) {
        if (input == null) return "";

        return input
            .replaceAll("(?i)&[0-9a-fk-or]", "")
            .replaceAll("(?i)§[0-9a-fk-or]", "")
            .replaceAll("(?i)<[^>]+>", "")
            .trim();
    }

    @Override
    public List<String> suggest(CommandSource source, String label, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            List<String> out = new java.util.ArrayList<>();
            if ("all".startsWith(prefix)) out.add("all");

            for (var server : proxy.platform().registeredServers()) {
                if (server.name().toLowerCase(Locale.ROOT).startsWith(prefix)) out.add(server.name());
            }

            return out;
        }

        return List.of();
    }
}
