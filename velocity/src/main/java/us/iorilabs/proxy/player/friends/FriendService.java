package us.iorilabs.proxy.player.friends;

import com.google.gson.JsonObject;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import us.iorilabs.proxy.commands.CommandSource;
import us.iorilabs.proxy.config.FriendsConfig;
import us.iorilabs.proxy.database.StorageService;
import us.iorilabs.proxy.database.mysql.MySqlStorageService;
import us.iorilabs.proxy.language.LanguageManager;
import us.iorilabs.proxy.logging.ProxyLogger;
import us.iorilabs.proxy.platform.ProxyPlatform;
import us.iorilabs.proxy.player.PlayerPresence;
import us.iorilabs.proxy.player.ProxyPlayer;
import us.iorilabs.proxy.player.friends.enums.FriendNotify;
import us.iorilabs.proxy.redis.ProxyEventType;
import us.iorilabs.proxy.redis.RedisEvent;
import us.iorilabs.proxy.redis.RedisService;
import us.iorilabs.proxy.server.PresenceService;
import us.iorilabs.proxy.utilities.ColorUtil;
import us.iorilabs.proxy.utilities.PageUtil;
import us.iorilabs.proxy.utilities.TextNames;

public final class FriendService {

    private final ProxyPlatform platform;
    private final RedisService redis;
    private final LanguageManager language;
    private final PresenceService presence;
    private final StorageService storage;
    private final ProxyLogger logger;
    private final String proxyId;
    private FriendsConfig config;
    private final Map<UUID, Set<UUID>> friendsByUuid = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> outgoingRequests = new ConcurrentHashMap<>();
    private final Map<UUID, Long> requestCooldownMs = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> pendingRemove = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastServerByUuid = new ConcurrentHashMap<>();
    private final Set<String> memoryFriendPairs = ConcurrentHashMap.newKeySet();
    private final Set<String> memoryRequestPairs = ConcurrentHashMap.newKeySet();

    public FriendService(
        ProxyPlatform platform,
        RedisService redis,
        LanguageManager language,
        PresenceService presence,
        StorageService storage,
        ProxyLogger logger,
        String proxyId,
        FriendsConfig config
    ) {
        this.platform = platform;
        this.redis = redis;
        this.language = language;
        this.presence = presence;
        this.storage = storage;
        this.logger = logger;
        this.proxyId = proxyId;
        this.config = config;
        redis.subscribe(ProxyEventType.FRIEND_REQUEST.id(), this::onRemoteRequest);
        redis.subscribe(ProxyEventType.FRIEND_ACCEPT.id(), this::onRemoteAccept);
        redis.subscribe(ProxyEventType.FRIEND_REMOVE.id(), this::onRemoteRemove);
        redis.subscribe(ProxyEventType.FRIEND_NOTIFY.id(), this::onRemoteNotify);
        loadFromDatabase();
    }

    public void reloadConfig(FriendsConfig config) {
        this.config = config;
    }

    public Optional<String> addRequest(UUID fromUuid, String fromName, String targetName) {
        if (targetName == null || targetName.isBlank()) return Optional.of("friends.usage_add");

        Optional<UUID> targetUuid = resolveUuid(targetName);
        if (targetUuid.isEmpty()) return Optional.of("general.player_not_found");

        UUID toUuid = targetUuid.get();
        if (fromUuid.equals(toUuid)) return Optional.of("friends.self");
        if (areFriends(fromUuid, toUuid)) return Optional.of("friends.already_friends");

        if (hasOutgoingRequest(fromUuid, toUuid) || hasOutgoingRequest(toUuid, fromUuid)) {
            return Optional.of("friends.pending");
        }

        Optional<Long> cooldown = cooldownRemainingSeconds(fromUuid);
        if (cooldown.isPresent()) return Optional.of("friends.cooldown:" + cooldown.get());
        ProxyPlayer fromPlayer = platform.findPlayerByUuid(fromUuid).orElse(null);
        ProxyPlayer toPlayer = platform.findPlayerByUuid(toUuid).orElse(null);
        if (friendCount(fromUuid) >= config.maxFriends(fromPlayer)) return Optional.of("friends.max_reached");
        if (friendCount(toUuid) >= config.maxFriends(toPlayer)) return Optional.of("friends.target_max");

        storeRequest(fromUuid, toUuid);
        requestCooldownMs.put(fromUuid, System.currentTimeMillis());
        publishRequest(fromUuid, fromName, toUuid, targetName);
        notifyIncomingRequest(toUuid, fromName);
        return Optional.empty();
    }

    public Optional<String> accept(UUID toUuid, String fromName) {
        Optional<UUID> fromUuid = resolveUuid(fromName);
        if (fromUuid.isEmpty()) return Optional.of("general.player_not_found");

        UUID from = fromUuid.get();
        if (!hasOutgoingRequest(from, toUuid)) return Optional.of("friends.no_request");

        ProxyPlayer toPlayer = platform.findPlayerByUuid(toUuid).orElse(null);
        ProxyPlayer fromPlayer = platform.findPlayerByUuid(from).orElse(null);
        if (friendCount(toUuid) >= config.maxFriends(toPlayer) || friendCount(from) >= config.maxFriends(fromPlayer)) {
            return Optional.of("friends.max_reached");
        }

        removeRequest(from, toUuid);
        addFriendship(from, toUuid);
        publishAccept(from, fromName, toUuid);
        notifyAccepted(from, toUuid);
        return Optional.empty();
    }

    public Optional<String> deny(UUID toUuid, String fromName) {
        Optional<UUID> fromUuid = resolveUuid(fromName);
        if (fromUuid.isEmpty()) return Optional.of("general.player_not_found");

        UUID from = fromUuid.get();
        if (!hasOutgoingRequest(from, toUuid)) return Optional.of("friends.no_request");

        removeRequest(from, toUuid);
        platform
            .findPlayerByUuid(from)
            .ifPresent(player ->
                player.sendMessage(language.raw("friends.denied_notify", Map.of("player", playerName(toUuid))))
            );
        return Optional.empty();
    }

    public Optional<String> cancelRequest(UUID fromUuid, String targetName) {
        Optional<UUID> targetUuid = resolveUuid(targetName);
        if (targetUuid.isEmpty()) return Optional.of("general.player_not_found");

        UUID to = targetUuid.get();
        if (!hasOutgoingRequest(fromUuid, to)) return Optional.of("friends.no_request");

        removeRequest(fromUuid, to);
        return Optional.empty();
    }

    public List<UUID> incomingRequests(UUID toUuid) {
        List<UUID> out = new ArrayList<>();
        for (Map.Entry<UUID, Set<UUID>> entry : outgoingRequests.entrySet()) {
            if (entry.getValue() != null && entry.getValue().contains(toUuid)) {
                out.add(entry.getKey());
            }
        }
        return out;
    }

    public void sendIncomingRequests(UUID toUuid, CommandSource source) {
        sendIncomingRequests(toUuid, source, 1);
    }

    public void sendIncomingRequests(UUID toUuid, CommandSource source, int page) {
        List<UUID> incoming = incomingRequests(toUuid);
        incoming.sort((a, b) -> playerName(a).compareToIgnoreCase(playerName(b)));

        int totalPages = PageUtil.totalPages(Math.max(incoming.size(), 1), PageUtil.DEFAULT_PAGE_SIZE);
        int safePage = PageUtil.clampPage(page, totalPages);

        for (String line : language.rawList(
            "friends.requests_header",
            Map.of(
                "count",
                String.valueOf(incoming.size()),
                "page",
                String.valueOf(safePage),
                "pages",
                String.valueOf(totalPages)
            )
        )) {
            source.sendMessage(line);
        }

        if (incoming.isEmpty()) {
            source.sendMessage(language.raw("friends.requests_empty"));
        } else {
            for (UUID from : PageUtil.slice(incoming, safePage, PageUtil.DEFAULT_PAGE_SIZE)) {
                String name = playerName(from);
                source.sendMessage(language.raw("friends.requests_entry", Map.of("player", name)));
                source.sendMessage(requestButtonsComponent(name));
            }
        }

        for (String line : language.rawList("friends.requests_footer")) {
            source.sendMessage(line);
        }

        if (!incoming.isEmpty() && totalPages > 1) {
            source.sendMessage(
                PageUtil.navBar(
                    "/friend requests",
                    safePage,
                    totalPages,
                    language.raw(
                        "general.page_label",
                        Map.of("page", String.valueOf(safePage), "pages", String.valueOf(totalPages))
                    ),
                    language.raw("general.help_prev_hover"),
                    language.raw("general.help_next_hover")
                )
            );
        }
    }

    private Component requestButtonsComponent(String playerName) {
        String acceptLabel = language.raw("friends.request_accept_button");
        if (acceptLabel.equals("friends.request_accept_button")) acceptLabel = "[ACEPTAR]";
        String denyLabel = language.raw("friends.request_deny_button");
        if (denyLabel.equals("friends.request_deny_button")) denyLabel = "[RECHAZAR]";
        String acceptHover = language.raw("friends.request_accept_hover");
        if (acceptHover.equals("friends.request_accept_hover")) acceptHover = "Aceptar solicitud";
        String denyHover = language.raw("friends.request_deny_hover");
        if (denyHover.equals("friends.request_deny_hover")) denyHover = "Rechazar solicitud";

        return Component.text("  ")
            .append(
                PageUtil.actionButton(
                    acceptLabel,
                    NamedTextColor.GREEN,
                    ClickEvent.runCommand("/friend accept " + playerName),
                    acceptHover
                )
            )
            .append(Component.text(" "))
            .append(
                PageUtil.actionButton(
                    denyLabel,
                    NamedTextColor.RED,
                    ClickEvent.runCommand("/friend deny " + playerName),
                    denyHover
                )
            );
    }

    public Optional<String> remove(UUID whoUuid, String friendName, boolean confirmArg) {
        Optional<UUID> friendUuid = resolveUuid(friendName);
        if (friendUuid.isEmpty()) return Optional.of("general.player_not_found");

        UUID friend = friendUuid.get();
        if (!areFriends(whoUuid, friend)) return Optional.of("friends.not_friend");

        if (config.confirmRemove() && !confirmArg) {
            UUID pending = pendingRemove.get(whoUuid);
            if (pending == null || !pending.equals(friend)) {
                pendingRemove.put(whoUuid, friend);
                return Optional.of("friends.remove_confirm");
            }
        }

        pendingRemove.remove(whoUuid);
        removeFriendship(whoUuid, friend);
        publishRemove(whoUuid, friend);
        platform
            .findPlayerByUuid(friend)
            .ifPresent(player ->
                player.sendMessage(language.raw("friends.removed_notify", Map.of("player", playerName(whoUuid))))
            );
        return Optional.empty();
    }

    public void sendList(UUID uuid, CommandSource source) {
        sendList(uuid, source, 1);
    }

    public void sendList(UUID uuid, CommandSource source, int page) {
        List<UUID> friends = new ArrayList<>(friendsOf(uuid));
        friends.sort((a, b) -> playerName(a).compareToIgnoreCase(playerName(b)));
        if (friends.isEmpty()) {
            for (String line : language.rawList("friends.list_empty")) {
                source.sendMessage(line);
            }
            return;
        }

        int totalPages = PageUtil.totalPages(friends.size(), PageUtil.DEFAULT_PAGE_SIZE);
        int safePage = PageUtil.clampPage(page, totalPages);

        for (String line : language.rawList(
            "friends.list_header",
            Map.of(
                "count",
                String.valueOf(friends.size()),
                "page",
                String.valueOf(safePage),
                "pages",
                String.valueOf(totalPages)
            )
        )) {
            source.sendMessage(line);
        }

        for (UUID friendUuid : PageUtil.slice(friends, safePage, PageUtil.DEFAULT_PAGE_SIZE)) {
            source.sendMessage(friendListEntry(friendUuid));
        }

        for (String line : language.rawList("friends.list_footer")) {
            source.sendMessage(line);
        }

        if (totalPages > 1) {
            source.sendMessage(
                PageUtil.navBar(
                    "/friend list",
                    safePage,
                    totalPages,
                    language.raw(
                        "general.page_label",
                        Map.of("page", String.valueOf(safePage), "pages", String.valueOf(totalPages))
                    ),
                    language.raw("general.help_prev_hover"),
                    language.raw("general.help_next_hover")
                )
            );
        }
    }

    private Component friendListEntry(UUID friendUuid) {
        String name = playerName(friendUuid);
        Optional<PlayerPresence> online = presence.findByUuid(friendUuid);
        if (online.isEmpty()) {
            online = platform
                .findPlayerByUuid(friendUuid)
                .map(p ->
                    new PlayerPresence(p.uuid(), p.name(), p.currentServer(), proxyId, System.currentTimeMillis())
                );
        }

        String base;
        if (online.isPresent() && online.get().server() != null && !online.get().server().isBlank()) {
            base = language.raw(
                "friends.list_entry_online",
                Map.of("player", name, "server", TextNames.displayServer(online.get().server()))
            );
        } else {
            base = language.raw("friends.list_entry_offline", Map.of("player", name));
        }

        String msgHover = language.raw("friends.list_msg_hover");
        if (msgHover.equals("friends.list_msg_hover")) msgHover = "Enviar mensaje";
        String removeHover = language.raw("friends.list_remove_hover");
        if (removeHover.equals("friends.list_remove_hover")) removeHover = "Eliminar amigo";
        String msgLabel = language.raw("friends.list_msg_button");
        if (msgLabel.equals("friends.list_msg_button")) msgLabel = "&a✎";
        String removeLabel = language.raw("friends.list_remove_button");
        if (removeLabel.equals("friends.list_remove_button")) removeLabel = "&c✖";

        return ColorUtil.parse(base)
            .append(Component.text(" "))
            .append(
                PageUtil.actionButton(
                    msgLabel,
                    NamedTextColor.AQUA,
                    ClickEvent.suggestCommand("/f msg " + name + " "),
                    msgHover
                )
            )
            .append(Component.text(" "))
            .append(
                PageUtil.actionButton(
                    removeLabel,
                    NamedTextColor.RED,
                    ClickEvent.runCommand("/friend remove " + name),
                    removeHover
                )
            );
    }

    public Optional<String> sendPrivateMessage(UUID fromUuid, String fromName, String targetName, String message) {
        if (message == null || message.isBlank()) return Optional.of("friends.msg_usage");

        Optional<UUID> targetUuid = resolveUuid(targetName);
        if (targetUuid.isEmpty()) return Optional.of("general.player_not_found");

        UUID toUuid = targetUuid.get();
        if (!areFriends(fromUuid, toUuid)) return Optional.of("friends.msg_not_friend");

        String resolvedTarget = playerName(toUuid);
        String outbound = language.raw("friends.msg_format_out", Map.of("player", resolvedTarget, "message", message));
        platform.findPlayerByUuid(fromUuid).ifPresent(p -> p.sendMessage(outbound));
        String inbound = language.raw("friends.msg_format_in", Map.of("player", fromName, "message", message));
        platform
            .findPlayerByUuid(toUuid)
            .ifPresentOrElse(p -> p.sendMessage(inbound), () -> sourceOfflineMessage(toUuid, fromName, message));
        spyStaff(fromName, resolvedTarget, message);
        return Optional.empty();
    }

    public Optional<String> broadcastToFriends(UUID fromUuid, String fromName, String message) {
        if (message == null || message.isBlank()) return Optional.of("friends.broadcast_usage");

        Set<UUID> friends = friendsOf(fromUuid);
        if (friends.isEmpty()) return Optional.of("friends.list_empty");

        String outbound = language.raw("friends.broadcast_format_out", Map.of("message", message));
        platform.findPlayerByUuid(fromUuid).ifPresent(p -> p.sendMessage(outbound));

        String inbound = language.raw("friends.broadcast_format_in", Map.of("player", fromName, "message", message));
        for (UUID friendUuid : friends) {
            platform.findPlayerByUuid(friendUuid).ifPresent(p -> p.sendMessage(inbound));
        }

        spyStaff(fromName, "*", message);
        return Optional.empty();
    }

    private void spyStaff(String from, String to, String message) {
        String perm =
            us.iorilabs.proxy.IoriProxy.get() == null
                ? "ioriproxy.friends.spy"
                : us.iorilabs.proxy.IoriProxy.get().configs().commands().permission("friends_spy");
        String line = language.raw("friends.msg_spy", Map.of("from", from, "to", to, "message", message));
        for (ProxyPlayer online : platform.onlinePlayers()) {
            if (online.hasPermission(perm) && !online.name().equalsIgnoreCase(from)) {
                online.sendMessage(line);
            }
        }
    }

    public void onLocalJoin(UUID uuid, String name, String server) {
        upsertPlayerAsync(uuid, name);
        if (server != null && !server.isBlank()) {
            lastServerByUuid.put(uuid, server);
        }

        if (!config.notifyJoin()) return;

        notifyLocalFriends(uuid, name, server, "", FriendNotify.JOIN);
        publishNotify(uuid, name, server, "", FriendNotify.JOIN);
    }

    public void onLocalQuit(UUID uuid, String name) {
        lastServerByUuid.remove(uuid);
        if (!config.notifyQuit()) return;

        notifyLocalFriends(uuid, name, "", "", FriendNotify.QUIT);
        publishNotify(uuid, name, "", "", FriendNotify.QUIT);
    }

    public void onLocalSwitch(UUID uuid, String name, String server) {
        String from = lastServerByUuid.getOrDefault(uuid, "?");
        if (server != null && !server.isBlank()) {
            lastServerByUuid.put(uuid, server);
        }

        if (from.equalsIgnoreCase(server)) return;
        if (!config.notifyServerSwitch()) return;

        notifyLocalFriends(uuid, name, server, from, FriendNotify.SWITCH);
        publishNotify(uuid, name, server, from, FriendNotify.SWITCH);
    }

    public List<String> suggestFriendNames(UUID uuid, String prefix) {
        String lower = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();

        for (UUID friend : friendsOf(uuid)) {
            String name = playerName(friend);
            if (name.toLowerCase(Locale.ROOT).startsWith(lower)) {
                out.add(name);
            }
        }

        return out;
    }

    public Optional<Long> cooldownRemainingSeconds(UUID uuid) {
        Long last = requestCooldownMs.get(uuid);
        if (last == null) return Optional.empty();

        long cooldownMs = config.requestCooldownSeconds() * 1000L;
        long elapsed = System.currentTimeMillis() - last;
        if (elapsed >= cooldownMs) return Optional.empty();

        return Optional.of((cooldownMs - elapsed + 999) / 1000);
    }

    private void notifyLocalFriends(UUID actorUuid, String actorName, String server, String from, FriendNotify kind) {
        for (UUID friendUuid : friendsOf(actorUuid)) {
            platform
                .findPlayerByUuid(friendUuid)
                .ifPresent(friend -> friend.sendMessage(notifyMessage(kind, actorName, server, from)));
        }
    }

    private void onRemoteNotify(RedisEvent event) {
        if (proxyId.equals(event.originProxyId())) return;

        JsonObject data = event.data();
        if (!data.has("uuid") || !data.has("kind")) return;

        UUID actor = UUID.fromString(data.get("uuid").getAsString());
        String name = data.has("name") ? data.get("name").getAsString() : "?";
        String server = data.has("server") ? data.get("server").getAsString() : "";
        String from = data.has("from") ? data.get("from").getAsString() : "";
        FriendNotify kind = FriendNotify.fromString(data.get("kind").getAsString());
        if (!notifyEnabled(kind)) return;

        for (UUID friendUuid : friendsOf(actor)) {
            platform
                .findPlayerByUuid(friendUuid)
                .ifPresent(friend -> friend.sendMessage(notifyMessage(kind, name, server, from)));
        }
    }

    private boolean notifyEnabled(FriendNotify kind) {
        return switch (kind) {
            case JOIN -> config.notifyJoin();
            case QUIT -> config.notifyQuit();
            case SWITCH -> config.notifyServerSwitch();
        };
    }

    private String notifyMessage(FriendNotify kind, String name, String server, String from) {
        String key = switch (kind) {
            case JOIN -> "friends.notify_join";
            case QUIT -> "friends.notify_quit";
            case SWITCH -> "friends.notify_switch";
        };
        return language.raw(
            key,
            Map.of(
                "player",
                name == null ? "?" : name,
                "server",
                TextNames.displayServer(server),
                "from",
                TextNames.displayServer(from)
            )
        );
    }

    private void notifyIncomingRequest(UUID toUuid, String fromName) {
        Map<String, String> ph = Map.of("player", fromName);
        platform.findPlayerByUuid(toUuid).ifPresent(player -> {
            player.sendMessage(language.raw("friends.request_received", ph));
            player.sendMessage(requestButtonsComponent(fromName));
        });
    }

    private void notifyAccepted(UUID fromUuid, UUID toUuid) {
        platform
            .findPlayerByUuid(fromUuid)
            .ifPresent(player ->
                player.sendMessage(language.raw("friends.accepted_self", Map.of("player", playerName(toUuid))))
            );
    }

    private void onRemoteRequest(RedisEvent event) {
        if (proxyId.equals(event.originProxyId())) return;

        JsonObject data = event.data();
        if (!data.has("from_uuid") || !data.has("to_uuid")) return;

        UUID from = UUID.fromString(data.get("from_uuid").getAsString());
        UUID to = UUID.fromString(data.get("to_uuid").getAsString());
        storeRequestMemory(from, to);
        persistRequest(from, to);
        String fromName = data.has("from_name") ? data.get("from_name").getAsString() : playerName(from);
        notifyIncomingRequest(to, fromName);
    }

    private void onRemoteAccept(RedisEvent event) {
        if (proxyId.equals(event.originProxyId())) return;

        JsonObject data = event.data();
        if (!data.has("from_uuid") || !data.has("to_uuid")) return;

        UUID from = UUID.fromString(data.get("from_uuid").getAsString());
        UUID to = UUID.fromString(data.get("to_uuid").getAsString());
        removeRequestMemory(from, to);
        deleteRequest(from, to);
        addFriendshipMemory(from, to);
        persistFriendship(from, to);
        notifyAccepted(from, to);
    }

    private void onRemoteRemove(RedisEvent event) {
        JsonObject data = event.data();
        if (!data.has("uuid_a") || !data.has("uuid_b")) return;

        UUID a = UUID.fromString(data.get("uuid_a").getAsString());
        UUID b = UUID.fromString(data.get("uuid_b").getAsString());
        removeFriendshipMemory(a, b);
        deleteFriendship(a, b);
    }

    private void publishRequest(UUID from, String fromName, UUID to, String toName) {
        JsonObject data = new JsonObject();
        data.addProperty("from_uuid", from.toString());
        data.addProperty("from_name", fromName);
        data.addProperty("to_uuid", to.toString());
        data.addProperty("to_name", toName);
        redis.publish(new RedisEvent(ProxyEventType.FRIEND_REQUEST.id(), proxyId, data));
    }

    private void publishAccept(UUID from, String fromName, UUID to) {
        JsonObject data = new JsonObject();
        data.addProperty("from_uuid", from.toString());
        data.addProperty("from_name", fromName);
        data.addProperty("to_uuid", to.toString());
        redis.publish(new RedisEvent(ProxyEventType.FRIEND_ACCEPT.id(), proxyId, data));
    }

    private void publishRemove(UUID a, UUID b) {
        FriendPair pair = FriendPair.ordered(a, b);
        JsonObject data = new JsonObject();
        data.addProperty("uuid_a", pair.uuidA().toString());
        data.addProperty("uuid_b", pair.uuidB().toString());
        redis.publish(new RedisEvent(ProxyEventType.FRIEND_REMOVE.id(), proxyId, data));
    }

    private void publishNotify(UUID uuid, String name, String server, String from, FriendNotify kind) {
        JsonObject data = new JsonObject();
        data.addProperty("uuid", uuid.toString());
        data.addProperty("name", name);
        data.addProperty("server", server == null ? "" : server);
        data.addProperty("from", from == null ? "" : from);
        data.addProperty("kind", kind.name());
        redis.publish(new RedisEvent(ProxyEventType.FRIEND_NOTIFY.id(), proxyId, data));
    }

    private void sourceOfflineMessage(UUID toUuid, String fromName, String message) {
        logger.module("Friends", "PM to offline " + playerName(toUuid));
    }

    public Optional<UUID> resolveUuidPublic(String name) {
        return resolveUuid(name);
    }

    public void transferAccount(UUID from, UUID to) {
        Set<UUID> friends = new java.util.HashSet<>(friendsOf(from));
        for (UUID friend : friends) {
            removeFriendshipMemory(from, friend);
            deleteFriendship(from, friend);
            if (!areFriends(to, friend) && !to.equals(friend)) {
                addFriendship(to, friend);
            }
        }

        Set<UUID> outgoing = outgoingRequests.getOrDefault(from, Set.of());
        for (UUID target : List.copyOf(outgoing)) {
            removeRequest(from, target);
            if (!hasOutgoingRequest(to, target) && !to.equals(target)) {
                storeRequest(to, target);
            }
        }

        for (Map.Entry<UUID, Set<UUID>> entry : List.copyOf(outgoingRequests.entrySet())) {
            if (entry.getValue() != null && entry.getValue().contains(from)) {
                removeRequest(entry.getKey(), from);
                if (!hasOutgoingRequest(entry.getKey(), to) && !entry.getKey().equals(to)) {
                    storeRequest(entry.getKey(), to);
                }
            }
        }

        friendsByUuid.remove(from);
        outgoingRequests.remove(from);
    }

    private Optional<UUID> resolveUuid(String name) {
        if (name == null || name.isBlank()) return Optional.empty();

        Optional<ProxyPlayer> local = platform.findPlayerByName(name);
        if (local.isPresent()) return Optional.of(local.get().uuid());

        Optional<PlayerPresence> remote = presence.findByName(name);
        if (remote.isPresent()) return Optional.of(remote.get().uuid());

        return loadUuidFromDatabase(name);
    }

    private void upsertPlayerAsync(UUID uuid, String name) {
        if (uuid == null || name == null || name.isBlank() || name.length() > 16) return;
        if (!storage.isConnected()) return;

        platform.asyncExecutor().execute(() ->
            storage.mysql().ifPresent(mysql -> {
                long now = System.currentTimeMillis();

                try {
                    mysql.call(connection -> {
                        try (
                            PreparedStatement ps = connection.prepareStatement(
                                """
                                INSERT INTO ip_players (uuid, name, first_join, last_join) VALUES (?, ?, ?, ?)
                                ON DUPLICATE KEY UPDATE name = VALUES(name), last_join = VALUES(last_join)
                                """
                            )
                        ) {
                            ps.setString(1, uuid.toString());
                            ps.setString(2, name);
                            ps.setLong(3, now);
                            ps.setLong(4, now);
                            ps.executeUpdate();
                            return null;
                        } catch (SQLException ex) {
                            throw new RuntimeException(ex);
                        }
                    });
                } catch (Exception ex) {
                    logger.error("Friends player upsert failed for " + uuid, ex);
                }
            })
        );
    }

    private Optional<UUID> loadUuidFromDatabase(String name) {
        Optional<MySqlStorageService> mysql = storage.mysql();
        if (mysql.isEmpty() || !storage.isConnected()) return Optional.empty();

        try {
            UUID found = mysql.get().call(connection -> {
                try {
                    try (
                        PreparedStatement ps = connection.prepareStatement(
                            "SELECT uuid FROM ip_players WHERE name = ? LIMIT 1"
                        )
                    ) {
                        ps.setString(1, name);

                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) return UUID.fromString(rs.getString("uuid"));
                        }
                    }

                    return null;
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            });
            return Optional.ofNullable(found);
        } catch (SQLException ex) {
            logger.error("Friends UUID lookup failed", ex);
            return Optional.empty();
        }
    }

    private String playerName(UUID uuid) {
        return platform
            .findPlayerByUuid(uuid)
            .map(ProxyPlayer::name)
            .or(() -> presence.findByUuid(uuid).map(PlayerPresence::name))
            .orElse(uuid.toString().substring(0, 8));
    }

    private int friendCount(UUID uuid) {
        return friendsOf(uuid).size();
    }

    private Set<UUID> friendsOf(UUID uuid) {
        return friendsByUuid.getOrDefault(uuid, Collections.emptySet());
    }

    private boolean areFriends(UUID a, UUID b) {
        Set<UUID> set = friendsByUuid.get(a);
        return set != null && set.contains(b);
    }

    private boolean hasOutgoingRequest(UUID from, UUID to) {
        Set<UUID> set = outgoingRequests.get(from);
        return set != null && set.contains(to);
    }

    private void storeRequest(UUID from, UUID to) {
        storeRequestMemory(from, to);
        persistRequest(from, to);
    }

    private void storeRequestMemory(UUID from, UUID to) {
        outgoingRequests.computeIfAbsent(from, k -> ConcurrentHashMap.newKeySet()).add(to);
        memoryRequestPairs.add(from + ":" + to);
    }

    private void removeRequest(UUID from, UUID to) {
        removeRequestMemory(from, to);
        deleteRequest(from, to);
    }

    private void removeRequestMemory(UUID from, UUID to) {
        Set<UUID> set = outgoingRequests.get(from);
        if (set != null) {
            set.remove(to);
        }

        memoryRequestPairs.remove(from + ":" + to);
    }

    private void addFriendship(UUID a, UUID b) {
        addFriendshipMemory(a, b);
        persistFriendship(a, b);
    }

    private void addFriendshipMemory(UUID a, UUID b) {
        friendsByUuid.computeIfAbsent(a, k -> ConcurrentHashMap.newKeySet()).add(b);
        friendsByUuid.computeIfAbsent(b, k -> ConcurrentHashMap.newKeySet()).add(a);
        memoryFriendPairs.add(FriendPair.ordered(a, b).memoryKey());
    }

    private void removeFriendship(UUID a, UUID b) {
        removeFriendshipMemory(a, b);
        deleteFriendship(a, b);
    }

    private void removeFriendshipMemory(UUID a, UUID b) {
        Set<UUID> setA = friendsByUuid.get(a);
        if (setA != null) {
            setA.remove(b);
        }

        Set<UUID> setB = friendsByUuid.get(b);
        if (setB != null) {
            setB.remove(a);
        }

        memoryFriendPairs.remove(FriendPair.ordered(a, b).memoryKey());
    }

    private void loadFromDatabase() {
        Optional<MySqlStorageService> mysql = storage.mysql();
        if (mysql.isEmpty() || !storage.isConnected()) {
            loadMemoryOnly();
            return;
        }

        try {
            mysql.get().call(connection -> {
                try {
                    try (
                        PreparedStatement ps = connection.prepareStatement("SELECT uuid_a, uuid_b FROM ip_friends");
                        ResultSet rs = ps.executeQuery()
                    ) {
                        while (rs.next()) {
                            UUID a = UUID.fromString(rs.getString("uuid_a"));
                            UUID b = UUID.fromString(rs.getString("uuid_b"));
                            addFriendshipMemory(a, b);
                        }
                    }

                    try (
                        PreparedStatement ps = connection.prepareStatement(
                            "SELECT from_uuid, to_uuid FROM ip_friend_requests"
                        );
                        ResultSet rs = ps.executeQuery()
                    ) {
                        while (rs.next()) {
                            UUID from = UUID.fromString(rs.getString("from_uuid"));
                            UUID to = UUID.fromString(rs.getString("to_uuid"));
                            storeRequestMemory(from, to);
                        }
                    }

                    return true;
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            });
        } catch (SQLException ex) {
            logger.error("Failed loading friends from MySQL — using memory fallback", ex);
            loadMemoryOnly();
        }
    }

    private void loadMemoryOnly() {
        for (String key : memoryFriendPairs) {
            String[] parts = key.split(":");
            if (parts.length == 2) {
                addFriendshipMemory(UUID.fromString(parts[0]), UUID.fromString(parts[1]));
            }
        }

        for (String key : memoryRequestPairs) {
            String[] parts = key.split(":");
            if (parts.length == 2) {
                storeRequestMemory(UUID.fromString(parts[0]), UUID.fromString(parts[1]));
            }
        }
    }

    private void persistRequest(UUID from, UUID to) {
        Optional<MySqlStorageService> mysql = storage.mysql();
        if (mysql.isEmpty() || !storage.isConnected()) return;

        try {
            mysql.get().call(connection -> {
                try {
                    try (
                        PreparedStatement ps = connection.prepareStatement(
                            """
                            INSERT INTO ip_friend_requests (from_uuid, to_uuid, created_at)
                            VALUES (?, ?, ?)
                            ON DUPLICATE KEY UPDATE created_at = VALUES(created_at)
                            """
                        )
                    ) {
                        ps.setString(1, from.toString());
                        ps.setString(2, to.toString());
                        ps.setLong(3, System.currentTimeMillis());
                        ps.executeUpdate();
                    }

                    return true;
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            });
        } catch (SQLException ex) {
            logger.error("Failed persisting friend request", ex);
        }
    }

    private void deleteRequest(UUID from, UUID to) {
        Optional<MySqlStorageService> mysql = storage.mysql();
        if (mysql.isEmpty() || !storage.isConnected()) return;

        try {
            mysql.get().call(connection -> {
                try {
                    try (
                        PreparedStatement ps = connection.prepareStatement(
                            "DELETE FROM ip_friend_requests WHERE from_uuid = ? AND to_uuid = ?"
                        )
                    ) {
                        ps.setString(1, from.toString());
                        ps.setString(2, to.toString());
                        ps.executeUpdate();
                    }

                    return true;
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            });
        } catch (SQLException ex) {
            logger.error("Failed deleting friend request", ex);
        }
    }

    private void persistFriendship(UUID a, UUID b) {
        FriendPair pair = FriendPair.ordered(a, b);
        Optional<MySqlStorageService> mysql = storage.mysql();
        if (mysql.isEmpty() || !storage.isConnected()) return;

        try {
            mysql.get().call(connection -> {
                try {
                    try (
                        PreparedStatement ps = connection.prepareStatement(
                            """
                            INSERT INTO ip_friends (uuid_a, uuid_b, since)
                            VALUES (?, ?, ?)
                            ON DUPLICATE KEY UPDATE since = since
                            """
                        )
                    ) {
                        ps.setString(1, pair.uuidA().toString());
                        ps.setString(2, pair.uuidB().toString());
                        ps.setLong(3, System.currentTimeMillis());
                        ps.executeUpdate();
                    }

                    return true;
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            });
        } catch (SQLException ex) {
            logger.error("Failed persisting friendship", ex);
        }
    }

    private void deleteFriendship(UUID a, UUID b) {
        FriendPair pair = FriendPair.ordered(a, b);
        Optional<MySqlStorageService> mysql = storage.mysql();
        if (mysql.isEmpty() || !storage.isConnected()) return;

        try {
            mysql.get().call(connection -> {
                try {
                    try (
                        PreparedStatement ps = connection.prepareStatement(
                            "DELETE FROM ip_friends WHERE uuid_a = ? AND uuid_b = ?"
                        )
                    ) {
                        ps.setString(1, pair.uuidA().toString());
                        ps.setString(2, pair.uuidB().toString());
                        ps.executeUpdate();
                    }

                    return true;
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            });
        } catch (SQLException ex) {
            logger.error("Failed deleting friendship", ex);
        }
    }
}
