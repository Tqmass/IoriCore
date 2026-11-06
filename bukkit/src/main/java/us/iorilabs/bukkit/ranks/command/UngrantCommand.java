package us.iorilabs.bukkit.ranks.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.language.CoreLanguage;
import us.iorilabs.bukkit.modules.core.EssentialsPlayers;
import us.iorilabs.bukkit.ranks.Rank;
import us.iorilabs.bukkit.ranks.RankService;
import us.iorilabs.proxy.commands.CommandParent;
import us.iorilabs.proxy.commands.CommandSource;

@Getter
@Accessors(fluent = true)
public final class UngrantCommand extends CommandParent {

    private final CoreLanguage language;
    private final RankService ranks;

    public UngrantCommand(CoreLanguage language, RankService ranks) {
        super("ungrant", Collections.emptyList(), "ioricore.ranks.ungrant");
        this.language = language;
        this.ranks = ranks;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (!source.hasPermission("ioricore.ranks.ungrant") && !source.hasPermission("ioricore.admin")) {
            source.sendMessage(language.raw(source, "GENERAL.NO-PERMISSION"));
            return;
        }
        if (args.length < 2) {
            source.sendMessage(language.raw(source, "RANKS.UNGRANT-USAGE"));
            return;
        }
        if (!ranks.grants().available()) {
            source.sendMessage(language.raw(source, "RANKS.GRANTS-UNAVAILABLE"));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        UUID playerId = target.getUniqueId();
        Rank rank = ranks.resolve(args[1]).orElse(null);
        if (rank == null) {
            source.sendMessage(language.raw(source, "RANKS.UNKNOWN", "rank", args[1]));
            return;
        }
        String reason = join(args, 2);
        if (reason.isEmpty()) reason = "Removed";

        if (!ranks.grants().loaded(playerId)) ranks.grants().load(playerId);
        int removed = ranks.ungrant(playerId, rank, source.name(), reason);
        if (removed <= 0) {
            source.sendMessage(language.raw(source, "RANKS.UNGRANT-NONE", "player", args[0], "rank", rank.key()));
            return;
        }
        source.sendMessage(
            language.raw(
                source,
                "RANKS.UNGRANT-OK",
                "player",
                target.getName() == null ? args[0] : target.getName(),
                "rank",
                rank.key(),
                "reason",
                reason
            )
        );
    }

    @Override
    public List<String> suggest(CommandSource source, String label, String[] args) {
        if (args.length <= 1) return EssentialsPlayers.suggestOnline(source, args.length == 0 ? "" : args[0]);
        if (args.length == 2) {
            String needle = args[1].toLowerCase(Locale.ROOT);
            List<String> out = new ArrayList<>();
            for (Rank rank : ranks.all()) {
                if (needle.isEmpty() || rank.key().startsWith(needle)) out.add(rank.key());
            }
            return out;
        }
        return Collections.emptyList();
    }

    @Override
    protected void sendUsage(CommandSource source) {
        source.sendMessage(language.raw(source, "RANKS.UNGRANT-USAGE"));
    }

    @Override
    protected String noPermissionMessage() {
        return language.raw("GENERAL.NO-PERMISSION");
    }

    private static String join(String[] args, int from) {
        if (args == null || from >= args.length) return "";
        StringBuilder out = new StringBuilder();
        for (int i = from; i < args.length; i++) {
            if (i > from) out.append(' ');
            out.append(args[i]);
        }
        return out.toString().trim();
    }
}
