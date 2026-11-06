package us.iorilabs.proxy.commands;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import us.iorilabs.proxy.IoriProxy;
import us.iorilabs.proxy.player.PlayerPresence;
import us.iorilabs.proxy.player.ProfileLookup;
import us.iorilabs.proxy.player.ProxyPlayer;
import us.iorilabs.proxy.utilities.DurationParser;
import us.iorilabs.proxy.utilities.TextNames;

public final class ProfileCommand implements ProxyCommand {

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(
        ZoneId.systemDefault()
    );

    private final IoriProxy proxy;
    private final ProfileLookup lookup;

    public ProfileCommand(IoriProxy proxy) {
        this.proxy = proxy;
        this.lookup = new ProfileLookup(proxy.storage(), proxy.logger());
    }

    @Override
    public String name() {
        return "profile";
    }

    @Override
    public String permission() {
        return proxy.configs().commands().permission("profile");
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        String targetName;
        if (args.length >= 1) {
            if (!source.hasPermission(proxy.configs().commands().permission("profile-other"))) {
                source.sendMessage(proxy.language().raw("general.no_permission"));
                return;
            }
            targetName = args[0];
        } else if (source.isPlayer()) {
            targetName = source.name();
        } else {
            source.sendMessage(proxy.language().raw("profile.usage"));
            return;
        }

        int protocol =
            source.uuidOrNull() == null
                ? Integer.MAX_VALUE
                : proxy
                      .platform()
                      .findPlayerByUuid(source.uuidOrNull())
                      .map(ProxyPlayer::protocolVersion)
                      .orElse(Integer.MAX_VALUE);
        proxy
            .platform()
            .asyncExecutor()
            .execute(() -> render(source, targetName, protocol));
    }

    private void render(CommandSource source, String targetName, int protocol) {
        Optional<ProxyPlayer> local = proxy.platform().findPlayerByName(targetName);
        Optional<PlayerPresence> remote = local.isPresent()
            ? Optional.empty()
            : proxy.presence().findByName(targetName);

        UUID uuid;
        String name;
        String server = null;
        if (local.isPresent()) {
            uuid = local.get().uuid();
            name = local.get().name();
            server = local.get().currentServer();
        } else if (remote.isPresent()) {
            uuid = remote.get().uuid();
            name = remote.get().name();
            server = remote.get().server();
        } else {
            Optional<ProfileLookup.Known> known = lookup.findByName(targetName);
            if (known.isEmpty()) {
                source.sendMessage(text("general.player_not_found", Map.of("player", targetName), protocol));
                return;
            }
            uuid = known.get().uuid();
            name = known.get().name();
        }

        boolean online = server != null;
        long firstJoin = proxy.playerData() == null ? 0L : proxy.playerData().firstJoinMillis(uuid);
        long lastJoin = proxy.playerData() == null ? 0L : proxy.playerData().lastJoinMillis(uuid);
        long playtime = proxy.playtime() == null ? 0L : proxy.playtime().totalMillis(uuid);
        int friends = lookup.friendCount(uuid);

        String status = online
            ? text(
                  "profile.status_online",
                  Map.of("server", TextNames.displayServer(blank(server) ? "?" : server)),
                  protocol
              )
            : text("profile.status_offline", Map.of(), protocol);
        String discord =
            proxy.discordLinks() != null && proxy.discordLinks().discordId(uuid).isPresent()
                ? text("profile.discord_linked", Map.of(), protocol)
                : text("profile.discord_not_linked", Map.of(), protocol);

        Map<String, String> ph = Map.ofEntries(
            Map.entry("player", name),
            Map.entry("uuid", uuid.toString()),
            Map.entry("rank", resolveRank(uuid, local.orElse(null), protocol)),
            Map.entry("status", status),
            Map.entry("first", formatWhen(firstJoin)),
            Map.entry("last", online ? text("profile.now", Map.of(), protocol) : formatWhen(lastJoin)),
            Map.entry("playtime", DurationParser.format(playtime)),
            Map.entry("friends", friends < 0 ? "—" : String.valueOf(friends)),
            Map.entry("discord", discord)
        );

        for (String line : proxy.language().rawList("profile.lines", ph, protocol)) {
            source.sendMessage(line);
        }
    }

    private String resolveRank(UUID uuid, ProxyPlayer local, int protocol) {
        if (proxy.luckPerms() != null && proxy.luckPerms().isPresent()) {
            String prefix = proxy.luckPerms().getPrefix(uuid);
            if (!blank(prefix)) return prefix.trim();
        }
        if (local != null && proxy.staffGroups() != null) {
            String display = proxy.staffGroups().displayRank(local);
            if (!blank(display)) return display.trim();
        }
        return text("profile.rank_none", Map.of(), protocol);
    }

    private String text(String key, Map<String, String> placeholders, int protocol) {
        return proxy.language().raw(key, placeholders, protocol);
    }

    @Override
    public List<String> suggest(CommandSource source, String label, String[] args) {
        if (args.length != 1 || !source.hasPermission(proxy.configs().commands().permission("profile-other"))) {
            return List.of();
        }

        String prefix = args[0].toLowerCase(Locale.ROOT);
        return proxy
            .presence()
            .all()
            .stream()
            .map(PlayerPresence::name)
            .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(prefix))
            .limit(50)
            .toList();
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static String formatWhen(long epochMs) {
        return epochMs <= 0L ? "—" : WHEN.format(Instant.ofEpochMilli(epochMs));
    }
}
