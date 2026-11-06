package us.iorilabs.bukkit.discord;

import java.util.List;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import us.iorilabs.bukkit.logging.BukkitLogger;

public final class DiscordSlashCommands {

    private DiscordSlashCommands() {}

    public static void register(JDA jda, DiscordConfig config, BukkitLogger logger) {
        if (jda == null || config == null || !config.slashCommands()) return;

        try {
            var sync = Commands.slash("sync", "Link Minecraft account with a one-time code").addOption(
                OptionType.STRING,
                "code",
                "Alphanumeric OTC from /sync or /staffreg in-game",
                true
            );
            var staffreg = Commands.slash("staffreg", "Register staff Discord link with one-time code").addOption(
                OptionType.STRING,
                "code",
                "One-time code from /staffreg in-game",
                true
            );

            String guildId = config.resolveGuildId();
            if (guildId != null && !guildId.isBlank()) {
                var guild = jda.getGuildById(guildId);
                if (guild != null) {
                    guild.updateCommands().addCommands(List.of(sync, staffreg)).queue();
                    logger.module("Discord", "slash /sync /staffreg registered (guild)");
                    return;
                }
            }

            jda.updateCommands().addCommands(List.of(sync, staffreg)).queue();
            logger.module("Discord", "slash /sync /staffreg registered (global)");
        } catch (Exception ex) {
            logger.warn("Discord slash register failed: " + ex.getMessage());
        }
    }
}
