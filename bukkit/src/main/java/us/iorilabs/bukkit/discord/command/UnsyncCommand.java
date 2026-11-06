package us.iorilabs.bukkit.discord.command;

import java.util.Collections;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.commands.BukkitCommandSource;
import us.iorilabs.bukkit.discord.link.DiscordLinkService;
import us.iorilabs.bukkit.language.CoreLanguage;
import us.iorilabs.proxy.commands.CommandParent;
import us.iorilabs.proxy.commands.CommandSource;

@Getter
@Accessors(fluent = true)
public final class UnsyncCommand extends CommandParent {

    private final IoriCorePlugin plugin;
    private final CoreLanguage language;
    private final DiscordLinkService links;

    public UnsyncCommand(IoriCorePlugin plugin, DiscordLinkService links) {
        super("unsync", Collections.emptyList(), "ioricore.discord.unsync");
        this.plugin = plugin;
        this.language = plugin.language();
        this.links = links;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (!(source instanceof BukkitCommandSource)) {
            source.sendMessage(language.raw(source, "GENERAL.PLAYER-ONLY"));
            return;
        }

        Player player = ((BukkitCommandSource) source).playerOrNull();
        if (player == null) {
            source.sendMessage(language.raw(source, "GENERAL.PLAYER-ONLY"));
            return;
        }
        if (!links.available()) {
            player.sendMessage(language.rawFor(player, "DISCORD.UNAVAILABLE"));
            return;
        }

        links.unlink(player.getUniqueId(), removed ->
            player.sendMessage(language.rawFor(player, removed ? "DISCORD.UNLINKED" : "DISCORD.NOT-LINKED"))
        );
    }

    @Override
    protected void sendUsage(CommandSource source) {
        for (String line : language.list(source, "DISCORD.UNSYNC-USAGE")) {
            source.sendMessage(line);
        }
    }

    @Override
    protected String noPermissionMessage() {
        return language.raw("GENERAL.NO-PERMISSION");
    }
}
