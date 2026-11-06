package us.iorilabs.bukkit.discord;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.bukkit.Bukkit;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.discord.link.DiscordLinkService;
import us.iorilabs.bukkit.logging.BukkitLogger;

@Getter
@Accessors(fluent = true)
public final class DiscordBotService {

    private final IoriCorePlugin plugin;
    private final BukkitLogger logger;

    private volatile DiscordConfig config;
    private volatile DiscordLinkService links;
    private volatile StaffAuthDiscordBridge staffAuth;
    private volatile JDA jda;
    private final Map<String, JDA> extraBots = new ConcurrentHashMap<>();
    private final AtomicBoolean starting = new AtomicBoolean(false);

    public DiscordBotService(IoriCorePlugin plugin, DiscordConfig config) {
        this.plugin = plugin;
        this.logger = plugin.coreLogger();
        this.config = config;
    }

    public void bind(DiscordLinkService links, StaffAuthDiscordBridge staffAuth) {
        this.links = links;
        this.staffAuth = staffAuth;
    }

    public void start() {
        stop();
        DiscordConfig cfg = config;
        if (cfg == null || !cfg.enabled()) {
            logger.module("Discord", "bot disabled");
            return;
        }
        if (cfg.useBridgeOnly()) {
            logger.module("Discord", "BRIDGE-only · JDA owned by proxy");
            return;
        }

        String token = cfg.resolveToken();
        if (token == null || token.isBlank()) {
            logger.warn("Discord enabled but TOKEN/BOTS.PRIMARY empty — fail soft");
            return;
        }
        if (!starting.compareAndSet(false, true)) return;

        List<DiscordConfig.BotEntry> extras = extraBotsToStart(cfg);
        if (extras.isEmpty()) {
            logger.module("Discord", "PRIMARY-only (no extra BOTS with TOKEN)");
        } else {
            logger.module("Discord", "starting PRIMARY + " + extras.size() + " extra bot(s)");
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                DiscordLinkService linkService = links;
                StaffAuthDiscordBridge authBridge = staffAuth;
                DiscordGatewayListener listener = new DiscordGatewayListener(this, linkService, authBridge, logger);

                JDA built = buildJda(token, listener);
                built.awaitReady();
                this.jda = built;
                DiscordSlashCommands.register(built, cfg, logger);
                logger.module("Discord", "JDA connected as &f" + built.getSelfUser().getName());

                for (Map.Entry<String, DiscordConfig.BotEntry> entry : namedExtras(cfg).entrySet()) {
                    startExtraBot(entry.getKey(), entry.getValue());
                }
            } catch (Exception ex) {
                this.jda = null;
                logger.warn("Discord bot failed to start (fail soft): " + ex.getMessage());
            } finally {
                starting.set(false);
            }
        });
    }

    public void reload(DiscordConfig config) {
        this.config = config;
        start();
    }

    public void stop() {
        JDA current = this.jda;
        this.jda = null;
        if (current != null) {
            try {
                current.shutdown();
            } catch (Exception ex) {
                logger.warn("Discord bot shutdown: " + ex.getMessage());
            }
        }

        for (Map.Entry<String, JDA> entry : new ArrayList<>(extraBots.entrySet())) {
            extraBots.remove(entry.getKey());
            JDA extra = entry.getValue();
            if (extra == null) continue;
            try {
                extra.shutdown();
            } catch (Exception ex) {
                logger.warn("Discord extra bot shutdown (" + entry.getKey() + "): " + ex.getMessage());
            }
        }
    }

    public boolean ready() {
        return jda != null && config != null && config.enabled();
    }

    private void startExtraBot(String id, DiscordConfig.BotEntry entry) {
        if (entry == null || !entry.enabled() || !entry.hasToken()) return;
        try {
            JDA built = buildJda(entry.token(), null);
            built.awaitReady();
            extraBots.put(id, built);
            logger.module("Discord", "extra bot &f" + id + " &7connected as &f" + built.getSelfUser().getName());
        } catch (Exception ex) {
            logger.warn("Discord extra bot failed (" + id + "): " + ex.getMessage());
        }
    }

    private static JDA buildJda(String token, Object listener) throws InterruptedException {
        JDABuilder builder = JDABuilder.createDefault(token)
            .enableIntents(
                EnumSet.of(
                    GatewayIntent.GUILD_MESSAGES,
                    GatewayIntent.MESSAGE_CONTENT,
                    GatewayIntent.GUILD_MEMBERS,
                    GatewayIntent.DIRECT_MESSAGES
                )
            )
            .setMemberCachePolicy(MemberCachePolicy.ALL)
            .setChunkingFilter(ChunkingFilter.NONE);
        if (listener != null) builder.addEventListeners(listener);
        return builder.build();
    }

    private static List<DiscordConfig.BotEntry> extraBotsToStart(DiscordConfig cfg) {
        List<DiscordConfig.BotEntry> out = new ArrayList<>();
        for (DiscordConfig.BotEntry entry : namedExtras(cfg).values()) {
            out.add(entry);
        }
        return out;
    }

    private static Map<String, DiscordConfig.BotEntry> namedExtras(DiscordConfig cfg) {
        Map<String, DiscordConfig.BotEntry> out = new ConcurrentHashMap<>();
        for (String id : cfg.botIds()) {
            if (id == null) continue;
            if ("primary".equals(id.toLowerCase(Locale.ROOT))) continue;
            DiscordConfig.BotEntry entry = cfg.bot(id);
            if (entry == null || !entry.enabled() || !entry.hasToken()) continue;
            out.put(id, entry);
        }
        return out;
    }
}
