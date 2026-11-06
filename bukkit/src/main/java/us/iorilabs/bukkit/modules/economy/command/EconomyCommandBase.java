package us.iorilabs.bukkit.modules.economy.command;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.language.CoreLanguage;
import us.iorilabs.bukkit.modules.core.EssentialsPlayers;
import us.iorilabs.bukkit.modules.economy.EconomyService;
import us.iorilabs.proxy.commands.CommandParent;
import us.iorilabs.proxy.commands.CommandSource;

@Getter
@Accessors(fluent = true)
public abstract class EconomyCommandBase extends CommandParent {

    protected final IoriCorePlugin plugin;
    protected final CoreLanguage language;
    protected final EconomyService economy;

    protected EconomyCommandBase(
        IoriCorePlugin plugin,
        EconomyService economy,
        String name,
        List<String> aliases,
        String permission
    ) {
        super(name, aliases, permission);
        this.plugin = plugin;
        this.language = plugin.language();
        this.economy = economy;
    }

    protected Player requirePlayer(CommandSource source) {
        Player player = EssentialsPlayers.player(source);
        if (player == null) {
            source.sendMessage(language.raw(source, "GENERAL.PLAYER-ONLY"));
            return null;
        }
        return player;
    }

    protected boolean deny(CommandSource source, String permission) {
        if (source.hasPermission(permission) || source.hasPermission("ioricore.admin")) return false;
        source.sendMessage(language.raw(source, "GENERAL.NO-PERMISSION"));
        return true;
    }

    protected void send(CommandSource source, String key) {
        source.sendMessage(language.raw(source, key));
    }

    protected void send(CommandSource source, String key, Map<String, String> placeholders) {
        source.sendMessage(language.raw(source, key, placeholders));
    }

    protected void sendUsageLines(CommandSource source, String key) {
        List<String> lines = language.list(source, key);
        if (lines.isEmpty()) {
            source.sendMessage(language.raw(source, key));
            return;
        }
        for (String line : lines) {
            source.sendMessage(line);
        }
    }

    protected OfflinePlayer resolveTarget(String name) {
        if (name == null || name.trim().isEmpty()) return null;
        Player online = EssentialsPlayers.findOnline(name);
        if (online != null) return online;

        for (OfflinePlayer offline : Bukkit.getOfflinePlayers()) {
            if (offline.getName() != null && offline.getName().equalsIgnoreCase(name)) return offline;
        }
        return Bukkit.getOfflinePlayer(name);
    }

    protected Double parseAmount(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        try {
            double value = Double.parseDouble(raw.trim().replace(",", ""));
            if (Double.isNaN(value) || Double.isInfinite(value)) return null;
            return value;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    protected String nameOf(OfflinePlayer player) {
        if (player == null) return "?";
        return player.getName() == null ? player.getUniqueId().toString() : player.getName();
    }

    @Override
    protected String noPermissionMessage() {
        return language.raw("GENERAL.NO-PERMISSION");
    }

    @Override
    public List<String> suggest(CommandSource source, String label, String[] args) {
        if (args.length <= 1) return EssentialsPlayers.suggestOnline(source, args.length == 0 ? "" : args[0]);
        return Collections.emptyList();
    }
}
