package us.iorilabs.proxy.commands;

import java.util.List;
import java.util.Map;
import us.iorilabs.proxy.IoriProxy;
import us.iorilabs.proxy.player.ProxyPlayer;

public final class AlertCommand implements ProxyCommand {

    private final IoriProxy proxy;

    public AlertCommand(IoriProxy proxy) {
        this.proxy = proxy;
    }

    @Override
    public String name() {
        return "alert";
    }

    @Override
    public List<String> aliases() {
        return List.of();
    }

    @Override
    public String permission() {
        return proxy.configs().commands().permission("alert");
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (args.length < 1) {
            source.sendMessage(proxy.language().raw("alert.usage"));
            return;
        }

        String text = stripColors(String.join(" ", args).trim());
        if (text.isBlank()) {
            source.sendMessage(proxy.language().raw("alert.usage"));
            return;
        }

        int legacyMax = proxy.language().legacyProtocolMax();
        for (ProxyPlayer player : proxy.platform().onlinePlayers()) {
            boolean legacy = player.protocolVersion() <= legacyMax;
            for (String line : proxy
                .language()
                .rawList(legacy ? "alert.broadcast_legacy" : "alert.broadcast_modern", Map.of("message", text))) {
                player.sendMessage(line);
            }
        }

        source.sendMessage(
            proxy.language().raw("alert.sent", Map.of("count", String.valueOf(proxy.platform().onlinePlayers().size())))
        );
    }

    private static String stripColors(String input) {
        if (input == null) return "";

        return input
            .replaceAll("(?i)&[0-9a-fk-or]", "")
            .replaceAll("(?i)§[0-9a-fk-or]", "")
            .replaceAll("(?i)<[^>]+>", "")
            .trim();
    }
}
