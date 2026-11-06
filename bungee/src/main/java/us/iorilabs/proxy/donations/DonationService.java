package us.iorilabs.proxy.donations;

import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import us.iorilabs.proxy.config.DonationsConfig;
import us.iorilabs.proxy.language.LanguageManager;
import us.iorilabs.proxy.logging.ProxyLogger;
import us.iorilabs.proxy.platform.ProxyPlatform;
import us.iorilabs.proxy.player.ProxyPlayer;
import us.iorilabs.proxy.redis.ProxyEventType;
import us.iorilabs.proxy.redis.RedisEvent;
import us.iorilabs.proxy.redis.RedisService;
import us.iorilabs.proxy.utilities.PlayerHeadAscii;
import us.iorilabs.proxy.webhooks.WebhookRegistry;

public final class DonationService {

    private final ProxyPlatform platform;
    private final RedisService redis;
    private final LanguageManager language;
    private final WebhookRegistry webhooks;
    private final ProxyLogger logger;
    private final String proxyId;
    private DonationsConfig config;
    private volatile long ggWaveEndsAtMs;
    private boolean soundUnsupportedLogged;
    private final AtomicInteger ggCount = new AtomicInteger();
    private final Map<UUID, Integer> ggPerPlayer = new ConcurrentHashMap<>();
    private int collected;
    private int goalMeta;

    public DonationService(
        ProxyPlatform platform,
        RedisService redis,
        LanguageManager language,
        WebhookRegistry webhooks,
        ProxyLogger logger,
        String proxyId,
        DonationsConfig config
    ) {
        this.platform = platform;
        this.redis = redis;
        this.language = language;
        this.webhooks = webhooks;
        this.logger = logger;
        this.proxyId = proxyId;
        this.config = config;
        this.collected = 0;
        this.goalMeta = config.goalMetaFallback();
        redis.subscribe(ProxyEventType.DONATION_BROADCAST.id(), this::onRemoteBroadcast);
        redis.subscribe(ProxyEventType.GGWAVE_START.id(), this::onRemoteGgWave);
        logPaymentNotes();
    }

    public void reload(DonationsConfig config) {
        this.config = config;
        if (this.goalMeta <= 0) {
            this.goalMeta = config.goalMetaFallback();
        }
        logPaymentNotes();
    }

    public void applyStoreGoal(Integer meta, Integer collectedAmount) {
        if (meta != null && meta > 0) {
            this.goalMeta = meta;
        }

        if (collectedAmount != null && collectedAmount >= 0) {
            this.collected = collectedAmount;
        }
    }

    private void logPaymentNotes() {
        if (config.tebexEnabled() && !blank(config.tebexSecret())) {
            logger.module("Donations", "Tebex secret set — wire store webhook or /donation test");
        }

        if (config.builtByBitEnabled()) {
            logger.module(
                "Donations",
                "BuiltByBit " + (blank(config.builtByBitSecret()) ? "enabled — set secret" : "secret set")
            );
        }

        if (config.craftingStoreEnabled()) {
            logger.module(
                "Donations",
                "CraftingStore " + (blank(config.craftingStoreSecret()) ? "enabled — set secret" : "secret set")
            );
        }
    }

    public void broadcastDonation(String mode, String playerName, String packageName, String price) {
        int amount = parsePrice(price);
        if (config.goalEnabled() && amount > 0) {
            collected += amount;
        }

        Map<String, String> ph = placeholders(mode, playerName, packageName, price);
        broadcastDonationLines(ph);
        JsonObject data = new JsonObject();
        data.addProperty("mode", mode);
        data.addProperty("player", playerName);
        data.addProperty("package", packageName);
        data.addProperty("price", price);
        data.addProperty("collected", collected);
        data.addProperty("meta", goalMeta);
        redis.publish(new RedisEvent(ProxyEventType.DONATION_BROADCAST.id(), proxyId, data));
        webhooks.send(
            "donations",
            Map.of(
                "player",
                playerName,
                "package",
                packageName,
                "price",
                price,
                "mode",
                mode,
                "collected",
                String.valueOf(collected),
                "meta",
                String.valueOf(goalMeta)
            )
        );
        startGgWave();
    }

    private void onRemoteBroadcast(RedisEvent event) {
        if (proxyId.equals(event.originProxyId())) return;

        JsonObject data = event.data();
        String mode = data.has("mode") ? data.get("mode").getAsString() : "?";
        String player = data.has("player") ? data.get("player").getAsString() : "?";
        String pkg = data.has("package") ? data.get("package").getAsString() : "?";
        String price = data.has("price") ? data.get("price").getAsString() : "?";
        if (data.has("collected")) {
            collected = Math.max(collected, data.get("collected").getAsInt());
        }

        if (data.has("meta")) {
            goalMeta = Math.max(1, data.get("meta").getAsInt());
        }

        broadcastDonationLines(placeholders(mode, player, pkg, price));
        startGgWave(false);
    }

    private void onRemoteGgWave(RedisEvent event) {
        if (proxyId.equals(event.originProxyId())) return;

        int duration = event.data().has("duration")
            ? event.data().get("duration").getAsInt()
            : config.ggWaveDurationSeconds();
        startGgWaveLocal(duration);
    }

    public void startGgWave() {
        startGgWave(true);
    }

    public void startGgWave(boolean publish) {
        if (!config.ggWaveEnabled()) return;

        startGgWaveLocal(config.ggWaveDurationSeconds());

        if (publish) {
            JsonObject data = new JsonObject();
            data.addProperty("duration", config.ggWaveDurationSeconds());
            redis.publish(new RedisEvent(ProxyEventType.GGWAVE_START.id(), proxyId, data));
        }
    }

    private void startGgWaveLocal(int durationSeconds) {
        if (!config.ggWaveEnabled()) return;

        ggCount.set(0);
        ggPerPlayer.clear();
        ggWaveEndsAtMs = System.currentTimeMillis() + Math.max(1, durationSeconds) * 1000L;
    }

    public void startGgWaveManual() {
        startGgWave(true);
    }

    public boolean enabled() {
        return true;
    }

    public boolean isGgWaveActive() {
        return config.ggWaveEnabled() && System.currentTimeMillis() < ggWaveEndsAtMs;
    }

    public boolean tryGgTrigger(ProxyPlayer player, String chatMessage) {
        if (!isGgWaveActive()) return false;

        String trimmed = chatMessage.trim().toLowerCase(Locale.ROOT);
        Set<String> inputs = new HashSet<>(config.ggWaveInputs());
        if (!inputs.contains(trimmed)) return false;

        int perPlayer = ggPerPlayer.merge(player.uuid(), 1, Integer::sum);
        if (perPlayer > config.ggWaveMaxPerPlayer()) return true;

        int count = ggCount.incrementAndGet();
        if (count > config.ggWaveMaxTotal()) return true;

        JsonObject wave = new JsonObject();
        wave.addProperty("player", player.name());
        wave.addProperty("message", chatMessage.trim());
        wave.addProperty("rainbow", config.ggWaveRainbow());
        wave.addProperty("rainbowOnlyMessage", config.ggWaveRainbowOnlyMessage());
        redis.publish(new RedisEvent(ProxyEventType.GGWAVE_CHAT.id(), proxyId, wave));
        return true;
    }

    public String triggerWord() {
        List<String> inputs = config.ggWaveInputs();
        return inputs.isEmpty() ? "gg" : inputs.get(0);
    }

    private void broadcastDonationLines(Map<String, String> placeholders) {
        int legacyMax = language.legacyProtocolMax();
        List<String> modernLines = language.rawList("donations.broadcast_modern", placeholders);
        List<String> legacyLines = language.rawList("donations.broadcast", placeholders);

        for (ProxyPlayer online : platform.onlinePlayers()) {
            boolean legacy = online.protocolVersion() <= legacyMax;
            List<String> lines = !legacy && !modernLines.isEmpty() ? modernLines : legacyLines;
            for (String line : lines) {
                if (line != null && !line.isBlank()) {
                    online.sendMessage(line);
                }
            }
        }

        playBroadcastSound();
    }

    private Map<String, String> placeholders(String mode, String player, String packet, String price) {
        Map<String, String> ph = new HashMap<>();
        ph.put("mode", mode);
        ph.put("server", mode);
        ph.put("player", player);
        ph.put("package", packet);
        ph.put("packet", packet);
        ph.put("price", price);
        ph.put("store", config.storeUrl());
        ph.put("collected", String.valueOf(collected));
        ph.put("meta", String.valueOf(goalMeta));
        ph.put("bar", buildBar());

        String skin = blank(player) ? config.defaultSkin() : player;
        List<String> heads = PlayerHeadAscii.linesFor(skin);
        for (int i = 0; i < 8; i++) {
            ph.put("head_" + (i + 1), i < heads.size() ? heads.get(i) : "");
        }

        return ph;
    }

    private String buildBar() {
        if (!config.goalEnabled()) return "";

        int length = config.barLength();
        int filled = (int) Math.round((collected / (double) Math.max(1, goalMeta)) * length);
        filled = Math.max(0, Math.min(length, filled));
        String filledChar = language.raw("donations.bar_filled");
        String emptyChar = language.raw("donations.bar_empty");
        if (filledChar == null || filledChar.isBlank() || filledChar.equals("donations.bar_filled")) {
            filledChar = "<green>■</green>";
        }

        if (emptyChar == null || emptyChar.isBlank() || emptyChar.equals("donations.bar_empty")) {
            emptyChar = "<red>■</red>";
        }

        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < length; i++) {
            bar.append(i < filled ? filledChar : emptyChar);
        }

        return bar.toString();
    }

    private void playBroadcastSound() {
        if (!config.soundsEnabled()) return;

        float volume = config.soundVolume();
        float pitch = config.soundPitch();
        int legacyMax = language.legacyProtocolMax();

        for (ProxyPlayer player : platform.onlinePlayers()) {
            String sound = player.protocolVersion() <= legacyMax ? config.legacySound() : config.modernSound();
            if (sound.isBlank()) continue;

            if (!platform.playSound(player.uuid(), sound, volume, pitch) && !soundUnsupportedLogged) {
                soundUnsupportedLogged = true;
                logger.module(
                    "Donations",
                    "Could not play sound &f" + sound + "&7 (invalid key or unsupported platform)"
                );
            }
        }
    }

    private static int parsePrice(String price) {
        if (price == null || price.isBlank()) return 0;

        String digits = price.replaceAll("[^0-9]", "");
        if (digits.isBlank()) return 0;

        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
