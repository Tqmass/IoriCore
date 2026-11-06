package us.iorilabs.bukkit.discord.command;

import java.util.Collections;
import java.util.Map;
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
public final class SyncCommand extends CommandParent {

    private final IoriCorePlugin plugin;
    private final CoreLanguage language;
    private final DiscordLinkService links;

    public SyncCommand(IoriCorePlugin plugin, DiscordLinkService links) {
        super("sync", Collections.emptyList(), "ioricore.discord.sync");
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

        long wait = links.cooldownRemaining(player.getUniqueId());
        if (wait > 0L) {
            player.sendMessage(language.rawFor(player, "DISCORD.COOLDOWN", Map.of("SECONDS", String.valueOf(wait))));
            return;
        }

        plugin
            .getServer()
            .getScheduler()
            .runTaskAsynchronously(plugin, () -> {
                if (links.discordId(player.getUniqueId()).isPresent()) {
                    plugin
                        .getServer()
                        .getScheduler()
                        .runTask(plugin, () -> player.sendMessage(language.rawFor(player, "DISCORD.ALREADY-LINKED")));
                    return;
                }

                links.requestCode(player, code -> {
                    if (code.isEmpty()) {
                        player.sendMessage(language.rawFor(player, "DISCORD.UNAVAILABLE"));
                        return;
                    }

                    Map<String, String> ph = Map.of(
                        "CODE",
                        code.get(),
                        "MINUTES",
                        String.valueOf(Math.max(1, links.codeTtlSeconds() / 60))
                    );
                    var lines = language.listFor(player, "DISCORD.SYNC-CODE", ph);
                    if (lines.isEmpty()) {
                        player.sendMessage(language.rawFor(player, "DISCORD.SYNC-CODE", ph));
                    } else {
                        for (String line : lines) {
                            player.sendMessage(line);
                        }
                    }
                });
            });
    }

    @Override
    protected void sendUsage(CommandSource source) {
        var lines = language.list(source, "DISCORD.SYNC-USAGE");
        if (lines.isEmpty()) {
            source.sendMessage(language.raw(source, "DISCORD.SYNC-USAGE"));
            return;
        }
        for (String line : lines) {
            source.sendMessage(line);
        }
    }

    @Override
    protected String noPermissionMessage() {
        return language.raw("GENERAL.NO-PERMISSION");
    }
}
