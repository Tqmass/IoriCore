package us.iorilabs.bukkit.modules.staffauth;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import us.iorilabs.api.modules.ModuleId;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.discord.DiscordBotService;
import us.iorilabs.bukkit.discord.DiscordModule;
import us.iorilabs.bukkit.discord.StaffRegRedisPublisher;
import us.iorilabs.bukkit.discord.link.DiscordLinkService;
import us.iorilabs.staffreg.DiscordEmbedDto;
import us.iorilabs.staffreg.DiscordMessageRequest;
import us.iorilabs.bukkit.language.CoreLanguage;
import us.iorilabs.bukkit.modules.ModuleToggle;
import us.iorilabs.bukkit.security.PasswordHasher;
import us.iorilabs.staffreg.crypto.StaffPasswordHasher;

public final class StaffRegService {

    private final IoriCorePlugin plugin;
    private final CoreLanguage language;
    private final StaffRegStore store;
    private volatile String staffPermission;
    private volatile int codeTtlSeconds;
    private volatile int minPasswordLength;
    private volatile int maxLoginAttempts;
    private volatile long lockoutMillis;
    private final Set<UUID> loggedIn = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Deque<Long>> failedLogins = new ConcurrentHashMap<>();
    private volatile StaffAuthEnforceService enforce;

    public StaffRegService(IoriCorePlugin plugin, String staffPermission, int codeTtlSeconds) {
        this.plugin = plugin;
        this.language = plugin.language();
        this.store = new StaffRegStore(plugin);
        this.staffPermission =
            staffPermission == null || staffPermission.isBlank() ? "ioriproxy.staff" : staffPermission;
        this.codeTtlSeconds = Math.max(300, codeTtlSeconds);
        refreshLimits(plugin.settings());
    }

    public void refreshFromSettings(FileConfiguration settings) {
        if (settings == null) settings = plugin.settings();
        String node = settings.getString("STAFF-AUTH.STAFF-PERMISSION", staffPermission);
        if (node == null || node.isBlank()) node = staffPermission;
        this.staffPermission = node.trim();
        this.codeTtlSeconds = Math.max(300, settings.getInt("STAFF-AUTH.REG-CODE-TTL-SECONDS", codeTtlSeconds));
        refreshLimits(settings);
    }

    public void bindEnforce(StaffAuthEnforceService enforce) {
        this.enforce = enforce;
    }

    private void refreshLimits(FileConfiguration settings) {
        if (settings == null) return;
        this.minPasswordLength = Math.max(6, settings.getInt("STAFF-AUTH.MIN-PASSWORD-LENGTH", 8));
        this.maxLoginAttempts = Math.max(3, settings.getInt("STAFF-AUTH.MAX-LOGIN-ATTEMPTS", 5));
        this.lockoutMillis = Math.max(60L, settings.getLong("STAFF-AUTH.LOCKOUT-SECONDS", 300L)) * 1000L;
    }

    private boolean moduleEnabled() {
        return ModuleToggle.enabled(plugin.settings(), ModuleId.STAFF_AUTH);
    }

    public boolean isRegistered(UUID uuid) {
        return uuid != null && store.registered(uuid);
    }

    public boolean isLoggedIn(UUID uuid) {
        return uuid != null && loggedIn.contains(uuid);
    }

    public void onQuit(UUID uuid) {
        if (uuid == null) return;
        loggedIn.remove(uuid);
    }

    public void createByAdmin(Player actor, String nick, String password, Consumer<CreateResult> callback) {
        if (actor == null || callback == null) return;
        if (!moduleEnabled()) {
            callback.accept(CreateResult.fail("STAFF-AUTH.MODULE-DISABLED"));
            return;
        }
        if (!actor.hasPermission("royalctw.staffreg.create") && !actor.hasPermission("ioricore.staffreg.create")) {
            callback.accept(CreateResult.fail("GENERAL.NO-PERMISSION"));
            return;
        }
        if (!isLoggedIn(actor.getUniqueId())) {
            callback.accept(CreateResult.fail("STAFFREG.ADMIN-NOT-AUTHENTICATED"));
            return;
        }
        if (nick == null || !nick.matches("[A-Za-z0-9_]{3,16}")) {
            callback.accept(CreateResult.fail("STAFFREG.INVALID-NICK"));
            return;
        }
        int minLen = Math.max(12, minPasswordLength);
        if (password == null || password.length() < minLen) {
            callback.accept(CreateResult.fail("STAFFREG.PASSWORD-TOO-SHORT"));
            return;
        }
        String user = nick.trim().toLowerCase(Locale.ROOT);
        OfflinePlayer target = Bukkit.getOfflinePlayer(user);
        if (target == null || target.getUniqueId() == null) {
            callback.accept(CreateResult.fail("STAFFREG.NOT-FOUND"));
            return;
        }
        UUID uuid = target.getUniqueId();
        if (store.registered(uuid)) {
            callback.accept(CreateResult.fail("STAFFREG.ALREADY-REGISTERED"));
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String hash = StaffPasswordHasher.hash(password);
            Bukkit.getScheduler().runTask(plugin, () -> {
                store.savePassword(uuid, hash);
                store.markMustChange(uuid, true);
                plugin.getLogger().info("[StaffReg] created staff account for " + user + " by " + actor.getName());
                callback.accept(CreateResult.ok(user, uuid));
            });
        });
    }

    public void register(Player player, String password) {
        if (player == null) return;
        if (!ensureStaff(player)) return;
        if (store.registered(player.getUniqueId())) {
            player.sendMessage(language.rawFor(player, "STAFF-AUTH.ALREADY-REGISTERED"));
            return;
        }
        if (password == null || password.length() < minPasswordLength) {
            player.sendMessage(
                language.rawFor(
                    player,
                    "STAFF-AUTH.PASSWORD-TOO-SHORT",
                    Map.of("min", String.valueOf(minPasswordLength))
                )
            );
            return;
        }
        store.savePassword(player.getUniqueId(), PasswordHasher.hash(password));
        player.sendMessage(language.rawFor(player, "STAFF-AUTH.REGISTERED"));
    }

    public void login(Player player, String password) {
        if (player == null) return;
        if (!ensureStaff(player)) return;
        UUID uuid = player.getUniqueId();
        if (isLocked(uuid)) {
            player.sendMessage(language.rawFor(player, "STAFF-AUTH.LOGIN-LOCKED"));
            return;
        }
        if (!store.registered(uuid)) {
            player.sendMessage(language.rawFor(player, "STAFF-AUTH.NOT-REGISTERED"));
            return;
        }
        Optional<String> hash = store.passwordHash(uuid);
        boolean ok =
            hash.isPresent() &&
            (StaffPasswordHasher.verify(password, hash.get()) || PasswordHasher.verify(password, hash.get()));
        if (!ok) {
            recordFailure(uuid);
            player.sendMessage(language.rawFor(player, "STAFF-AUTH.LOGIN-WRONG"));
            return;
        }
        if (store.mustChangePassword(uuid)) {
            player.sendMessage(language.rawFor(player, "STAFFREG.MUST-CHANGE-PASSWORD"));
            return;
        }

        clearFailures(uuid);
        loggedIn.add(uuid);
        StaffAuthEnforceService localEnforce = enforce;
        if (localEnforce != null) localEnforce.acceptLocalLogin(uuid);
        player.sendMessage(language.rawFor(player, "STAFF-AUTH.LOGIN-OK"));
    }

    public void logout(Player player) {
        if (player == null) return;
        loggedIn.remove(player.getUniqueId());
        player.sendMessage(language.rawFor(player, "STAFF-AUTH.LOGGED-OUT"));
    }

    public void oauth(Player player) {
        if (player == null) return;
        if (!ensureStaff(player)) return;
        if (!store.registered(player.getUniqueId())) {
            player.sendMessage(language.rawFor(player, "STAFF-AUTH.NOT-REGISTERED"));
            return;
        }

        DiscordModule discord = discord();
        if (discordReady(discord) && !store.linked(player.getUniqueId())) {
            beginDiscordLink(player);
            return;
        }

        if (store.linked(player.getUniqueId())) {
            player.sendMessage(language.rawFor(player, "STAFF-AUTH.DISCORD-LINKED"));
        } else {
            player.sendMessage(language.rawFor(player, "STAFF-AUTH.DISCORD-UNAVAILABLE"));
        }
    }

    public void beginDiscordLink(Player player) {
        if (player == null) return;
        if (!ensureStaff(player)) return;

        DiscordModule discord = discord();
        if (!discordReady(discord)) {
            player.sendMessage(language.rawFor(player, "STAFF-AUTH.DISCORD-UNAVAILABLE"));
            return;
        }

        DiscordLinkService links = discord.links();
        Optional<String> discordId = links.discordId(player.getUniqueId());
        if (discordId.isPresent() && !discordId.get().isBlank()) {
            store.markLinked(player.getUniqueId(), discordId.get());
            player.sendMessage(language.rawFor(player, "STAFF-AUTH.DISCORD-LINKED"));
            notifyLinkedDm(discord, player, discordId.get());
            return;
        }

        Optional<String> existing = store.code(player.getUniqueId());
        if (existing.isPresent()) {
            sendCode(player, existing.get());
            return;
        }

        links.requestCode(player, optional -> {
            if (optional.isEmpty()) {
                player.sendMessage(language.rawFor(player, "STAFF-AUTH.DISCORD-LINK-FAILED"));
                return;
            }
            String code = optional.get();
            long expires = System.currentTimeMillis() + codeTtlSeconds * 1000L;
            store.savePending(player.getUniqueId(), code, expires);
            sendCode(player, code);
        });
    }

    public void onDiscordLinked(UUID uuid, String discordId, String name) {
        if (uuid == null || discordId == null || discordId.isBlank()) return;
        if (!moduleEnabled()) return;
        store.markLinked(uuid, discordId);

        Player online = Bukkit.getPlayer(uuid);
        if (online != null && online.isOnline()) {
            online.sendMessage(language.rawFor(online, "STAFF-AUTH.DISCORD-LINK-COMPLETE"));
        }

        DiscordModule discord = discord();
        if (discord != null && discord.staffRegPublisher() != null) {
            notifyLinkedDm(discord, online, discordId);
        }
    }

    private void notifyLinkedDm(DiscordModule discord, Player player, String discordId) {
        StaffRegRedisPublisher publisher = discord.staffRegPublisher();
        if (publisher == null) return;
        String playerName = player == null ? "?" : player.getName();
        Map<String, String> ph = Map.of("player", playerName, "discord", discordId);
        publisher.publish(
            new DiscordMessageRequest()
                .discordUserId(discordId)
                .embed(new DiscordEmbedDto().templateKey("staff-reg").placeholders(ph)),
            true
        );
    }

    private void sendCode(Player player, String code) {
        Map<String, String> ph = Map.of("CODE", code, "MINUTES", String.valueOf(Math.max(1, codeTtlSeconds / 60)));
        for (String line : language.listFor(player, "STAFF-AUTH.DISCORD-LINK-CODE", ph)) {
            player.sendMessage(line);
        }
    }

    private boolean ensureStaff(Player player) {
        if (!moduleEnabled()) {
            player.sendMessage(language.rawFor(player, "STAFF-AUTH.MODULE-DISABLED"));
            return false;
        }
        if (!player.hasPermission(staffPermission) && !player.hasPermission("ioricore.staff")) {
            player.sendMessage(language.rawFor(player, "STAFF-AUTH.NOT-STAFF"));
            return false;
        }
        return true;
    }

    private boolean discordReady(DiscordModule discord) {
        if (discord == null || !plugin.settings().getBoolean("MODULES.DISCORD", false)) return false;
        DiscordLinkService links = discord.links();
        DiscordBotService bot = discord.bot();
        return links != null && links.available() && bot != null && bot.ready();
    }

    private DiscordModule discord() {
        if (plugin.modules() == null) return null;
        return plugin.modules().get(ModuleId.DISCORD);
    }

    private boolean isLocked(UUID uuid) {
        Deque<Long> attempts = failedLogins.get(uuid);
        if (attempts == null) return false;
        long cutoff = System.currentTimeMillis() - lockoutMillis;
        attempts.removeIf(t -> t < cutoff);
        return attempts.size() >= maxLoginAttempts;
    }

    private void recordFailure(UUID uuid) {
        failedLogins.computeIfAbsent(uuid, k -> new ArrayDeque<>()).addLast(System.currentTimeMillis());
        store.incrementLoginFailures(uuid);
    }

    private void clearFailures(UUID uuid) {
        failedLogins.remove(uuid);
        store.clearLoginFailures(uuid);
    }

    public record CreateResult(boolean ok, String messageKey, String username, UUID uuid) {
        static CreateResult ok(String username, UUID uuid) {
            return new CreateResult(true, "STAFFREG.CREATED", username, uuid);
        }

        static CreateResult fail(String messageKey) {
            return new CreateResult(false, messageKey, null, null);
        }
    }
}
