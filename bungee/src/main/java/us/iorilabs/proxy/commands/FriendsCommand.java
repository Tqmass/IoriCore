package us.iorilabs.proxy.commands;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import us.iorilabs.proxy.IoriProxy;
import us.iorilabs.proxy.commands.argument.LambdaCommandArgument;
import us.iorilabs.proxy.player.ProxyPlayer;
import us.iorilabs.proxy.player.friends.FriendService;

public final class FriendsCommand extends CommandParent {

    private final IoriProxy proxy;
    private final FriendService friendService;

    public FriendsCommand(IoriProxy proxy, FriendService friendService) {
        super("friend", proxy.configs().commands().aliases("friend"), proxy.configs().commands().permission("friends"));
        this.proxy = proxy;
        this.friendService = friendService;
        register(new LambdaCommandArgument("add", null, this::handleAdd));
        register(new LambdaCommandArgument("accept", null, this::handleAccept));
        register(new LambdaCommandArgument("deny", null, this::handleDeny));
        register(new LambdaCommandArgument("remove", null, this::handleRemove));
        LambdaCommandArgument list = new LambdaCommandArgument("list", null, this::handleList);
        register(list);
        register(new LambdaCommandArgument("msg", null, this::handleMsg));
        register(new LambdaCommandArgument("broadcast", null, this::handleBroadcast));
        register(new LambdaCommandArgument("request", null, this::handleRequest));
        register(new LambdaCommandArgument("requests", null, this::handleRequests));
        register(new LambdaCommandArgument("help", null, (source, args) -> sendUsage(source)));
        setDefaultArgument(list);
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (args.length == 0) {
            handleList(source, args);
            return;
        }

        super.execute(source, label, args);
    }

    private void handleAdd(CommandSource source, String[] args) {
        if (!requirePlayer(source)) return;

        if (args.length < 1) {
            source.sendMessage(proxy.language().raw("friends.usage_add"));
            return;
        }

        UUID uuid = source.uuidOrNull();
        ProxyPlayer player = proxy.platform().findPlayerByUuid(uuid).orElse(null);
        if (player == null || uuid == null) return;

        dispatchError(source, friendService.addRequest(uuid, player.name(), args[0]), () ->
            source.sendMessage(proxy.language().raw("friends.request_sent", Map.of("player", args[0])))
        );
    }

    private void handleAccept(CommandSource source, String[] args) {
        if (!requirePlayer(source)) return;

        if (args.length < 1) {
            source.sendMessage(proxy.language().raw("friends.usage_accept"));
            return;
        }

        UUID uuid = source.uuidOrNull();
        if (uuid == null) return;

        dispatchError(source, friendService.accept(uuid, args[0]), () ->
            source.sendMessage(proxy.language().raw("friends.accepted", Map.of("player", args[0])))
        );
    }

    private void handleDeny(CommandSource source, String[] args) {
        if (!requirePlayer(source)) return;

        if (args.length < 1) {
            source.sendMessage(proxy.language().raw("friends.usage_deny"));
            return;
        }

        UUID uuid = source.uuidOrNull();
        if (uuid == null) return;

        dispatchError(source, friendService.deny(uuid, args[0]), () ->
            source.sendMessage(proxy.language().raw("friends.denied", Map.of("player", args[0])))
        );
    }

    private void handleRemove(CommandSource source, String[] args) {
        if (!requirePlayer(source)) return;

        if (args.length < 1) {
            source.sendMessage(proxy.language().raw("friends.usage_remove"));
            return;
        }

        UUID uuid = source.uuidOrNull();
        if (uuid == null) return;

        boolean confirm = args.length >= 2 && "confirm".equalsIgnoreCase(args[1]);
        String target = args[0];
        dispatchError(source, friendService.remove(uuid, target, confirm), () ->
            source.sendMessage(proxy.language().raw("friends.removed", Map.of("player", target)))
        );
    }

    private void handleList(CommandSource source, String[] args) {
        if (!requirePlayer(source)) return;

        UUID uuid = source.uuidOrNull();
        if (uuid == null) return;

        friendService.sendList(uuid, source, us.iorilabs.proxy.utilities.PageUtil.parsePage(args, 0));
    }

    private void handleMsg(CommandSource source, String[] args) {
        if (!requirePlayer(source)) return;

        if (args.length < 2) {
            source.sendMessage(proxy.language().raw("friends.msg_usage"));
            return;
        }

        UUID uuid = source.uuidOrNull();
        ProxyPlayer player = uuid == null ? null : proxy.platform().findPlayerByUuid(uuid).orElse(null);
        if (player == null) return;

        String target = args[0];
        String message = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        dispatchError(source, friendService.sendPrivateMessage(uuid, player.name(), target, message), () -> {});
    }

    private void handleBroadcast(CommandSource source, String[] args) {
        if (!requirePlayer(source)) return;

        if (!source.hasPermission(proxy.configs().commands().permission("friends_broadcast"))) {
            source.sendMessage(proxy.language().raw("general.no_permission"));
            return;
        }

        if (args.length < 1) {
            source.sendMessage(proxy.language().raw("friends.broadcast_usage"));
            return;
        }

        UUID uuid = source.uuidOrNull();
        ProxyPlayer player = uuid == null ? null : proxy.platform().findPlayerByUuid(uuid).orElse(null);
        if (player == null) return;

        String message = String.join(" ", args);
        dispatchError(source, friendService.broadcastToFriends(uuid, player.name(), message), () -> {});
    }

    private void handleRequest(CommandSource source, String[] args) {
        if (!requirePlayer(source)) return;

        if (args.length < 1) {
            source.sendMessage(proxy.language().raw("friends.usage_request"));
            return;
        }

        String action = args[0].toLowerCase();
        if ("cancel".equals(action)) {
            if (args.length < 2) {
                source.sendMessage(proxy.language().raw("friends.usage_request_cancel"));
                return;
            }

            UUID uuid = source.uuidOrNull();
            if (uuid == null) return;

            String target = args[1];
            dispatchError(source, friendService.cancelRequest(uuid, target), () ->
                source.sendMessage(proxy.language().raw("friends.request_cancelled", Map.of("player", target)))
            );
            return;
        }

        source.sendMessage(proxy.language().raw("friends.usage_request"));
    }

    private void handleRequests(CommandSource source, String[] args) {
        if (!requirePlayer(source)) return;

        UUID uuid = source.uuidOrNull();
        if (uuid == null) return;

        friendService.sendIncomingRequests(uuid, source, us.iorilabs.proxy.utilities.PageUtil.parsePage(args, 0));
    }

    private void dispatchError(CommandSource source, Optional<String> error, Runnable onSuccess) {
        if (error.isEmpty()) {
            onSuccess.run();
            return;
        }

        String key = error.get();
        if (key.startsWith("friends.cooldown:")) {
            String seconds = key.substring("friends.cooldown:".length());
            source.sendMessage(proxy.language().raw("friends.cooldown", Map.of("seconds", seconds)));
            return;
        }

        if (key.equals("general.player_not_found")) {
            source.sendMessage(proxy.language().raw("general.player_not_found", Map.of("player", "?")));
            return;
        }

        for (String line : proxy.language().rawList(key)) {
            source.sendMessage(line);
        }
    }

    private boolean requirePlayer(CommandSource source) {
        if (!source.isPlayer()) {
            source.sendMessage(proxy.language().raw("general.player_only"));
            return false;
        }

        return true;
    }

    @Override
    protected void sendUsage(CommandSource source) {
        for (String line : proxy.language().rawList("friends.help")) {
            source.sendMessage(line);
        }
    }

    @Override
    protected String noPermissionMessage() {
        return proxy.language().raw("general.no_permission");
    }

    @Override
    public List<String> suggest(CommandSource source, String label, String[] args) {
        if (!source.isPlayer()) return super.suggest(source, label, args);

        UUID uuid = source.uuidOrNull();
        if (uuid == null) return super.suggest(source, label, args);

        if (args.length == 2 && isPlayerArg(args[0])) return friendService.suggestFriendNames(uuid, args[1]);

        return super.suggest(source, label, args);
    }

    private static boolean isPlayerArg(String sub) {
        String lower = sub.toLowerCase();
        return (
            lower.equals("add") ||
            lower.equals("accept") ||
            lower.equals("deny") ||
            lower.equals("remove") ||
            lower.equals("msg") ||
            lower.equals("broadcast") ||
            lower.equals("request")
        );
    }
}
