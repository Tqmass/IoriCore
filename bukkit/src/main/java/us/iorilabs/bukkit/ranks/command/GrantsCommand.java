package us.iorilabs.bukkit.ranks.command;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
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
import us.iorilabs.bukkit.ranks.grant.Grant;
import us.iorilabs.proxy.commands.CommandParent;
import us.iorilabs.proxy.commands.CommandSource;

@Getter
@Accessors(fluent = true)
public final class GrantsCommand extends CommandParent {

    private static final SimpleDateFormat DATE = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ROOT);

    static {
        DATE.setTimeZone(TimeZone.getDefault());
    }

    private final CoreLanguage language;
    private final RankService ranks;

    public GrantsCommand(CoreLanguage language, RankService ranks) {
        super("grants", Collections.emptyList(), "ioricore.ranks.grants");
        this.language = language;
        this.ranks = ranks;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (!source.hasPermission("ioricore.ranks.grants") && !source.hasPermission("ioricore.admin")) {
            source.sendMessage(language.raw(source, "GENERAL.NO-PERMISSION"));
            return;
        }
        if (args.length < 1) {
            source.sendMessage(language.raw(source, "RANKS.GRANTS-USAGE"));
            return;
        }
        if (!ranks.grants().available()) {
            source.sendMessage(language.raw(source, "RANKS.GRANTS-UNAVAILABLE"));
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        UUID playerId = target.getUniqueId();
        String name = target.getName() == null ? args[0] : target.getName();
        List<Grant> history = ranks.grants().history(playerId, 20);
        source.sendMessage(
            language.raw(source, "RANKS.GRANTS-HEADER", "player", name, "count", String.valueOf(history.size()))
        );
        if (history.isEmpty()) {
            source.sendMessage(language.raw(source, "RANKS.GRANTS-EMPTY", "player", name));
            return;
        }
        long now = System.currentTimeMillis();
        for (Grant grant : history) {
            Rank rank = ranks.byId(grant.rankId()).orElse(null);
            String rankName = rank == null ? grant.rankId().toString() : rank.key();
            String state;
            if (grant.removed()) state = "removed";
            else if (grant.expired(now)) state = "expired";
            else state = "active";
            String expires = grant.permanent() ? "permanent" : DATE.format(new Date(grant.expiresAt()));
            source.sendMessage(
                language.raw(
                    source,
                    "RANKS.GRANTS-LINE",
                    "rank",
                    rankName,
                    "state",
                    state,
                    "by",
                    grant.addedBy() == null ? "?" : grant.addedBy(),
                    "reason",
                    grant.reason() == null ? "" : grant.reason(),
                    "expires",
                    expires,
                    "created",
                    DATE.format(new Date(grant.createdAt()))
                )
            );
        }
    }

    @Override
    public List<String> suggest(CommandSource source, String label, String[] args) {
        if (args.length <= 1) return EssentialsPlayers.suggestOnline(source, args.length == 0 ? "" : args[0]);
        return Collections.emptyList();
    }

    @Override
    protected void sendUsage(CommandSource source) {
        source.sendMessage(language.raw(source, "RANKS.GRANTS-USAGE"));
    }

    @Override
    protected String noPermissionMessage() {
        return language.raw("GENERAL.NO-PERMISSION");
    }
}
