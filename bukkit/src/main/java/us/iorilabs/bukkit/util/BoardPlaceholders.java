package us.iorilabs.bukkit.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import us.iorilabs.api.hook.LuckPermsHook;
import us.iorilabs.api.hook.VaultHook;
import us.iorilabs.api.modules.ModuleId;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.modules.CoreModule;
import us.iorilabs.bukkit.modules.core.vanish.VanishVisibility;
import us.iorilabs.bukkit.modules.economy.EconomyModule;
import us.iorilabs.bukkit.modules.economy.EconomyService;
import us.iorilabs.bukkit.ranks.RankDisplay;
import us.iorilabs.bukkit.ranks.RankService;
import us.iorilabs.bukkit.ranks.RanksModule;

public final class BoardPlaceholders {

    private BoardPlaceholders() {}

    public static Map<String, String> of(IoriCorePlugin plugin, Player player) {
        Map<String, String> map = new HashMap<>();
        if (player == null) return map;

        map.put("player", player.getName());
        map.put("online", String.valueOf(VanishVisibility.visibleOnlineCount(plugin, player)));
        map.put("max", String.valueOf(Bukkit.getMaxPlayers()));
        String server = plugin == null ? "lobby" : plugin.settings().getString("SETTINGS.SERVER-NAME", "lobby");
        map.put("server", server);
        map.put("brand", plugin == null ? "RoyalCTW" : plugin.settings().getString("TAB.TEXT.BRAND", "RoyalCTW"));
        map.put("promo", plugin == null ? "" : plugin.settings().getString("TAB.TEXT.PROMO", ""));
        String modeKey = "TAB.TEXT.MODES." + server.toUpperCase(Locale.ROOT).replace('.', '-');
        map.put(
            "mode",
            plugin == null
                ? "Lobby"
                : plugin.settings().getString(modeKey, plugin.settings().getString("TAB.TEXT.MODES.DEFAULT", "Lobby"))
        );
        map.put("ping", String.valueOf(safePing(player)));
        map.put("world", player.getWorld() == null ? "?" : player.getWorld().getName());
        map.put("time", new SimpleDateFormat("HH:mm:ss", Locale.ROOT).format(new Date()));

        RankDisplay display = rankDisplay(plugin, player);
        String prefix =
            display != null && display.prefix() != null && !display.prefix().isEmpty()
                ? display.prefix()
                : resolveFallbackPrefix(plugin, player);
        String suffix = display != null && display.suffix() != null ? display.suffix() : "";
        String rankName = display != null && display.display() != null ? display.display() : "";
        String color = display != null && display.color() != null ? display.color() : "";
        String weight = display == null ? "0" : String.valueOf(display.weight());

        map.put("rank", rankName);
        map.put("vault_prefix", prefix);
        map.put("vault-prefix", prefix);
        map.put("rank_prefix", prefix);
        map.put("rank_suffix", suffix);
        map.put("rank_display", rankName);
        map.put("rank_color", color);
        map.put("rank_weight", weight);

        map.put("friends", "-");
        map.put("kills", "0");
        map.put("deaths", "0");
        map.put("playtime", "-");
        map.put("playtime_raw", "-");
        map.put("first_join", "-");
        map.put("last_join", "-");
        map.put("first_connection", "-");

        EconomySnapshot economy = economySnapshot(plugin, player);
        map.put("coins", economy.formatted);
        map.put("balance", economy.formatted);
        map.put("balance_raw", economy.raw);
        map.put("gems", economy.gems);
        map.put("credits", economy.credits);
        return map;
    }

    private static EconomySnapshot economySnapshot(IoriCorePlugin plugin, Player player) {
        if (plugin == null || player == null || plugin.modules() == null) {
            return EconomySnapshot.empty();
        }
        CoreModule module = plugin.modules().get(ModuleId.ECONOMY);
        if (!(module instanceof EconomyModule) || !module.enabled()) return EconomySnapshot.empty();
        EconomyService economy = ((EconomyModule) module).service();
        if (economy == null) return EconomySnapshot.empty();
        double amount = economy.balance(player.getUniqueId());
        return new EconomySnapshot(economy.format(amount), formatRaw(amount), "0", "0");
    }

    private static String formatRaw(double amount) {
        if (amount == (long) amount) return String.valueOf((long) amount);
        return String.valueOf(amount);
    }

    private static RankDisplay rankDisplay(IoriCorePlugin plugin, Player player) {
        if (plugin == null || plugin.modules() == null || player == null) return null;
        CoreModule module = plugin.modules().get(ModuleId.RANKS);
        if (!(module instanceof RanksModule) || !module.enabled()) return null;
        RankService ranks = ((RanksModule) module).ranks();
        return ranks == null ? null : ranks.display(player);
    }

    private static String resolveFallbackPrefix(IoriCorePlugin plugin, Player player) {
        if (plugin == null || player == null) return "";
        VaultHook vault = plugin.vaultHook();
        if (vault != null && vault.available()) {
            String prefix = vault.prefix(player.getUniqueId());
            if (prefix != null && !prefix.isEmpty()) return prefix;
        }
        LuckPermsHook luckPerms = plugin.luckPermsHook();
        if (luckPerms != null && luckPerms.available()) {
            String prefix = luckPerms.prefix(player.getUniqueId());
            if (prefix != null) return prefix;
        }
        return "";
    }

    private static int safePing(Player player) {
        try {
            Object ping = player.getClass().getMethod("getPing").invoke(player);
            if (ping instanceof Integer) return (Integer) ping;
        } catch (ReflectiveOperationException ignored) {}
        try {
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            return handle.getClass().getField("ping").getInt(handle);
        } catch (ReflectiveOperationException | ClassCastException ignored) {
            return 0;
        }
    }

    private static final class EconomySnapshot {

        private final String formatted;
        private final String raw;
        private final String gems;
        private final String credits;

        private EconomySnapshot(String formatted, String raw, String gems, String credits) {
            this.formatted = formatted;
            this.raw = raw;
            this.gems = gems;
            this.credits = credits;
        }

        private static EconomySnapshot empty() {
            return new EconomySnapshot("0", "0", "0", "0");
        }
    }
}
