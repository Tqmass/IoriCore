package us.iorilabs.proxy.commands;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import us.iorilabs.proxy.IoriProxy;
import us.iorilabs.proxy.commands.argument.LambdaCommandArgument;
import us.iorilabs.proxy.helpop.HelpOpService;
import us.iorilabs.proxy.player.ProxyPlayer;

public final class RequestCommand extends CommandParent {

    private final IoriProxy proxy;
    private final HelpOpService helpOpService;

    public RequestCommand(IoriProxy proxy, HelpOpService helpOpService) {
        super(
            "request",
            proxy.configs().commands().aliases("request"),
            proxy.configs().commands().permission("request_send")
        );
        this.proxy = proxy;
        this.helpOpService = helpOpService;
        register(
            new LambdaCommandArgument(
                "reply",
                proxy.configs().commands().permission("request_reply"),
                this::handleReply
            )
        );
        register(new LambdaCommandArgument("move", null, this::handleMove));
        setDefaultArgument(
            new LambdaCommandArgument(
                "create",
                proxy.configs().commands().permission("request_send"),
                this::handleCreate
            )
        );
    }

    private void handleCreate(CommandSource source, String[] args) {
        if (!source.isPlayer()) {
            source.sendMessage(proxy.language().raw("general.player_only"));
            return;
        }

        if (args.length < 1) {
            source.sendMessage(proxy.language().raw("helpop.usage"));
            return;
        }

        UUID uuid = source.uuidOrNull();
        if (uuid == null) return;

        ProxyPlayer player = proxy.platform().findPlayerByUuid(uuid).orElse(null);
        if (player == null) return;

        Optional<Long> cooldown = helpOpService.cooldownRemainingSeconds(uuid);
        if (cooldown.isPresent()) {
            source.sendMessage(
                proxy.language().raw("helpop.cooldown", Map.of("seconds", String.valueOf(cooldown.get())))
            );
            return;
        }

        String message = String.join(" ", args);
        Optional<us.iorilabs.proxy.helpop.HelpOpTicket> ticket = helpOpService.create(player, message);
        if (ticket.isEmpty()) {
            source.sendMessage(proxy.language().raw("helpop.cooldown", Map.of("seconds", "0")));
            return;
        }

        source.sendMessage(proxy.language().raw("helpop.sent", Map.of("id", ticket.get().id())));
    }

    private void handleReply(CommandSource source, String[] args) {
        if (!source.isPlayer()) {
            source.sendMessage(proxy.language().raw("general.player_only"));
            return;
        }

        if (args.length < 2) {
            source.sendMessage(proxy.language().raw("helpop.reply_usage"));
            return;
        }

        UUID uuid = source.uuidOrNull();
        if (uuid == null) return;

        ProxyPlayer staff = proxy.platform().findPlayerByUuid(uuid).orElse(null);
        if (staff == null) return;

        String id = args[0];
        String reply = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        if (!helpOpService.reply(staff, id, reply)) {
            source.sendMessage(proxy.language().raw("helpop.not_found", Map.of("id", id)));
            return;
        }

        source.sendMessage(proxy.language().raw("helpop.replied_staff", Map.of("id", id, "player", staff.name())));
    }

    private void handleMove(CommandSource source, String[] args) {
        String movePerm = proxy.configs().commands().permission("request_move");
        String replyPerm = proxy.configs().commands().permission("request_reply");
        if (!source.hasPermission(movePerm) && !source.hasPermission(replyPerm)) {
            source.sendMessage(proxy.language().raw("general.no_permission"));
            return;
        }

        if (!source.isPlayer()) {
            source.sendMessage(proxy.language().raw("general.player_only"));
            return;
        }

        if (args.length < 1) {
            source.sendMessage(proxy.language().raw("helpop.move_usage"));
            return;
        }

        UUID uuid = source.uuidOrNull();
        if (uuid == null) return;

        ProxyPlayer staff = proxy.platform().findPlayerByUuid(uuid).orElse(null);
        if (staff == null) return;

        String id = args[0];
        if (!helpOpService.moveStaffToTicket(staff, id)) {
            source.sendMessage(proxy.language().raw("helpop.not_found", Map.of("id", id)));
            return;
        }

        source.sendMessage(proxy.language().raw("helpop.moved", Map.of("id", id)));
    }

    @Override
    protected void sendUsage(CommandSource source) {
        source.sendMessage(proxy.language().raw("helpop.usage"));
    }

    @Override
    protected String noPermissionMessage() {
        return proxy.language().raw("general.no_permission");
    }

    @Override
    public List<String> suggest(CommandSource source, String label, String[] args) {
        if (args.length == 1 && source.hasPermission(proxy.configs().commands().permission("request_reply"))) {
            String prefix = args[0].toLowerCase();
            if ("reply".startsWith(prefix)) return List.of("reply");
            if ("move".startsWith(prefix)) return List.of("move");
        }

        return super.suggest(source, label, args);
    }
}
