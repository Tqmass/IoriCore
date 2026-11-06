package us.iorilabs.proxy.announcements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import us.iorilabs.proxy.config.AnnouncementsConfig;
import us.iorilabs.proxy.language.LanguageManager;
import us.iorilabs.proxy.logging.ProxyLogger;
import us.iorilabs.proxy.placeholders.PlaceholderService;
import us.iorilabs.proxy.platform.ProxyPlatform;
import us.iorilabs.proxy.player.ProxyPlayer;
import us.iorilabs.proxy.redis.ProxyEventType;
import us.iorilabs.proxy.redis.RedisEvent;
import us.iorilabs.proxy.redis.RedisService;
import us.iorilabs.proxy.server.ServerModes;
import us.iorilabs.proxy.utilities.TextCenter;

public final class AnnouncementService {

    private static final String TASK_ID = "ioriproxy-announcements";
    private static final String DEFAULT_MODE = "DEFAULT";

    private final ProxyPlatform platform;
    private final RedisService redis;
    private final LanguageManager language;
    private final ProxyLogger logger;
    private final String proxyId;
    private final Random random = new Random();
    private final Map<String, Integer> modeIndex = new ConcurrentHashMap<>();
    private final Map<String, Integer> modeLastRandom = new ConcurrentHashMap<>();
    private PlaceholderService placeholders;
    private AnnouncementsConfig config;
    private Map<String, List<List<String>>> modernByMode = Map.of();
    private Map<String, List<List<String>>> legacyByMode = Map.of();
    private boolean soundUnsupportedLogged;

    public AnnouncementService(
        ProxyPlatform platform,
        RedisService redis,
        LanguageManager language,
        ProxyLogger logger,
        String proxyId,
        AnnouncementsConfig config
    ) {
        this.platform = platform;
        this.redis = redis;
        this.language = language;
        this.logger = logger;
        this.proxyId = proxyId;
        this.config = config;
        reloadMessages();
        redis.subscribe(ProxyEventType.ANNOUNCEMENT_RELOAD.id(), this::onReloadRemote);
    }

    public void setPlaceholders(PlaceholderService placeholders) {
        this.placeholders = placeholders;
    }

    public void start() {
        platform.cancelTask(TASK_ID);

        if (modernByMode.isEmpty() && legacyByMode.isEmpty()) {
            logger.module("Announcements", "no messages configured");
            return;
        }

        long periodMs = Math.max(5L, config.intervalSeconds()) * 1000L;
        platform.runTimer(this::tick, periodMs, periodMs, TASK_ID);
        logger.module(
            "Announcements",
            "timer every &f" +
                config.intervalSeconds() +
                "s &7(" +
                modernByMode.size() +
                " modes modern / " +
                legacyByMode.size() +
                " legacy)"
        );
    }

    public void stop() {
        platform.cancelTask(TASK_ID);
    }

    public void reload(AnnouncementsConfig config) {
        this.config = config;
        reloadMessages();
        start();
    }

    public void publishReloadSignal() {
        redis.publish(
            new RedisEvent(ProxyEventType.ANNOUNCEMENT_RELOAD.id(), proxyId, new com.google.gson.JsonObject())
        );
    }

    private void onReloadRemote(RedisEvent event) {
        if (proxyId.equals(event.originProxyId())) return;

        reloadMessages();
    }

    private void reloadMessages() {
        modernByMode = loadByMode(Integer.MAX_VALUE);
        legacyByMode = loadByMode(0);
        if (legacyByMode.isEmpty()) legacyByMode = modernByMode;
        modeIndex.clear();
        modeLastRandom.clear();
    }

    private Map<String, List<List<String>>> loadByMode(int protocol) {
        Map<String, List<List<String>>> out = new LinkedHashMap<>();
        Set<String> children = language.listChildKeys("announcements", protocol);
        if (children.isEmpty() && protocol <= language.legacyProtocolMax()) {
            children = language.listChildKeys("announcements", Integer.MAX_VALUE);
        }

        for (String child : children) {
            List<List<String>> blocks = parseBlocks(language.rawList("announcements." + child, protocol));
            if (!blocks.isEmpty()) {
                out.put(child.toUpperCase(Locale.ROOT), blocks);
            }
        }

        if (out.isEmpty()) {
            List<List<String>> flat = parseBlocks(language.rawList("announcements", protocol));
            if (!flat.isEmpty()) {
                out.put(DEFAULT_MODE, flat);
            }
        }

        return Map.copyOf(out);
    }

    private static List<List<String>> parseBlocks(List<String> blocks) {
        if (blocks == null || blocks.isEmpty()) return List.of();

        List<List<String>> out = new ArrayList<>();
        for (String block : blocks) {
            if (block == null || block.isBlank()) continue;

            String[] split = block.split("\\R", -1);
            List<String> lines = new ArrayList<>(split.length);
            for (String line : split) {
                lines.add(line);
            }
            out.add(List.copyOf(lines));
        }

        return List.copyOf(out);
    }

    private void tick() {
        Set<String> allowed = allowedServers();
        Map<String, Integer> picks = new HashMap<>();
        List<ProxyPlayer> recipients = new ArrayList<>();

        for (ProxyPlayer player : platform.onlinePlayers()) {
            String server = player.currentServer();
            if (!allowed.isEmpty()) {
                if (server == null || !allowed.contains(server.toLowerCase(Locale.ROOT))) continue;
            }

            String mode = resolveMode(server);
            boolean legacy = player.protocolVersion() <= language.legacyProtocolMax();
            List<List<String>> pool = poolFor(mode, legacy);
            if (pool.isEmpty()) continue;

            String pickKey = mode + (legacy ? ":L" : ":M");
            int pick = picks.computeIfAbsent(pickKey, k -> nextIndex(mode, pool.size()));
            List<String> lines = pool.get(pick % pool.size());
            for (String rawLine : lines) {
                String line = rawLine;
                if (line != null && !line.isBlank() && !line.toLowerCase(Locale.ROOT).contains("<center>")) {
                    line = "<center>" + line + "</center>";
                }
                String rendered = TextCenter.apply(line);
                if (placeholders != null) {
                    rendered = placeholders.apply(rendered, player);
                }
                sendAnnouncementLine(player, rendered);
            }
            recipients.add(player);
        }

        playSound(recipients);
    }

    private String resolveMode(String server) {
        if (server == null || server.isBlank()) return DEFAULT_MODE;

        return ServerModes.modeKey(server).toUpperCase(Locale.ROOT);
    }

    private List<List<String>> poolFor(String mode, boolean legacy) {
        Map<String, List<List<String>>> bag = legacy ? legacyByMode : modernByMode;
        List<List<String>> pool = bag.get(mode);
        if (pool != null && !pool.isEmpty()) return pool;

        pool = bag.get(DEFAULT_MODE);
        if (pool != null && !pool.isEmpty()) return pool;

        if (legacy && bag != modernByMode) {
            return poolFor(mode, false);
        }

        return Collections.emptyList();
    }

    private void sendAnnouncementLine(ProxyPlayer player, String line) {
        if (line == null || line.isBlank()) return;

        String trimmed = line.strip();
        if (trimmed.matches("(?i).*(https?://\\S+).*")) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?i)(https?://\\S+)").matcher(trimmed);
            if (m.find()) {
                String url = m.group(1);
                String before = trimmed.substring(0, m.start());
                String after = trimmed.substring(m.end());
                net.kyori.adventure.text.Component msg = net.kyori.adventure.text.Component.empty();
                if (!before.isBlank()) msg = msg.append(us.iorilabs.proxy.utilities.ColorUtil.parse(before));
                msg = msg.append(
                    net.kyori.adventure.text.Component.text(
                        url,
                        net.kyori.adventure.text.format.NamedTextColor.BLUE
                    ).clickEvent(net.kyori.adventure.text.event.ClickEvent.openUrl(url))
                );
                if (!after.isBlank()) msg = msg.append(us.iorilabs.proxy.utilities.ColorUtil.parse(after));
                player.sendMessage(msg);
                return;
            }
        }

        player.sendMessage(line);
    }

    private int nextIndex(String mode, int size) {
        if (size <= 0) return 0;

        if (size <= 1) return 0;

        if (!config.random()) {
            int current = modeIndex.getOrDefault(mode, 0);
            if (current >= size) current = 0;
            int last = modeLastRandom.getOrDefault(mode, -1);
            if (current == last && size > 1) current = (current + 1) % size;
            modeIndex.put(mode, current + 1);
            modeLastRandom.put(mode, current);
            return current;
        }

        int last = modeLastRandom.getOrDefault(mode, -1);
        int pick;
        do {
            pick = random.nextInt(size);
        } while (pick == last);

        modeLastRandom.put(mode, pick);
        return pick;
    }

    private void playSound(List<ProxyPlayer> recipients) {
        if (!config.soundEnabled() || recipients.isEmpty()) return;

        float volume = config.soundVolume();
        float pitch = config.soundPitch();
        int legacyMax = language.legacyProtocolMax();

        for (ProxyPlayer player : recipients) {
            String sound = player.protocolVersion() <= legacyMax ? config.legacySound() : config.modernSound();
            if (sound.isBlank()) continue;

            if (!platform.playSound(player.uuid(), sound, volume, pitch) && !soundUnsupportedLogged) {
                soundUnsupportedLogged = true;
                logger.module("Announcements", "Could not play sound &f" + sound);
            }
        }
    }

    private Set<String> allowedServers() {
        List<String> servers = config.servers();
        if (servers == null || servers.isEmpty()) return Collections.emptySet();

        return servers
            .stream()
            .map(s -> s.toLowerCase(Locale.ROOT))
            .collect(Collectors.toUnmodifiableSet());
    }
}
