package us.iorilabs.bukkit.discord;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import us.iorilabs.bukkit.discord.link.DiscordLinkService;
import us.iorilabs.bukkit.logging.BukkitLogger;

public final class DiscordGatewayListener extends ListenerAdapter {

    private final DiscordBotService bot;
    private final DiscordLinkService links;
    private final StaffAuthDiscordBridge staffAuth;
    private final BukkitLogger logger;

    public DiscordGatewayListener(
        DiscordBotService bot,
        DiscordLinkService links,
        StaffAuthDiscordBridge staffAuth,
        BukkitLogger logger
    ) {
        this.bot = bot;
        this.links = links;
        this.staffAuth = staffAuth;
        this.logger = logger;
    }

    @Override
    public void onReady(ReadyEvent event) {
        String name = event.getJDA().getSelfUser().getName();
        logger.module("Discord", "JDA ready as &f" + name);
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        DiscordConfig config = bot.config();
        if (config == null || !config.enabled() || !config.slashCommands()) return;
        String name = event.getName();
        if (!"sync".equalsIgnoreCase(name) && !"staffreg".equalsIgnoreCase(name)) return;

        OptionMapping option = event.getOption("code");
        String code = option == null ? "" : option.getAsString();
        event.deferReply(true).queue();

        bot.plugin()
            .getServer()
            .getScheduler()
            .runTaskAsynchronously(bot.plugin(), () -> {
                DiscordLinkService.Redeem redeem = links.redeem(code, event.getUser().getId());
                String reply = replyFor(redeem, config, event.getUser().getName());
                event.getHook().sendMessage(reply).queue();
            });
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String id = event.getComponentId();
        if (id == null) return;
        if (
            !id.startsWith(StaffAuthDiscordBridge.APPROVE_PREFIX) && !id.startsWith(StaffAuthDiscordBridge.DENY_PREFIX)
        ) {
            return;
        }

        DiscordConfig config = bot.config();
        if (config == null || !config.enabled()) {
            event
                .reply(config == null ? "Bot disabled." : config.reply("bot-disabled", "Bot disabled."))
                .setEphemeral(true)
                .queue();
            return;
        }

        Member member = resolveMember(event, config);
        String channelId = event.getChannel() == null ? "" : event.getChannel().getId();
        if (!mayUseCommands(member, channelId, config) && !hasStaffishPermission(member)) {
            event.reply(config.reply("staff-auth-role", "❌ Missing Discord staff role.")).setEphemeral(true).queue();
            return;
        }

        StaffAuthDiscordBridge.ButtonResult result = staffAuth.handleButton(id, event.getUser());
        switch (result) {
            case APPROVED -> {
                event
                    .reply(
                        DiscordEmbeds.apply(
                            config.reply("staff-auth-approved", "✅ Approved by **%actor%**."),
                            Map.of("actor", event.getUser().getName())
                        )
                    )
                    .queue();
                event.editButton(event.getButton().asDisabled()).queue(null, ignored -> {});
            }
            case DENIED -> {
                event
                    .reply(
                        DiscordEmbeds.apply(
                            config.reply("staff-auth-denied", "⛔ Denied by **%actor%**."),
                            Map.of("actor", event.getUser().getName())
                        )
                    )
                    .queue();
                event.editButton(event.getButton().asDisabled()).queue(null, ignored -> {});
            }
            case SELF_APPROVE -> event
                .reply(config.reply("staff-auth-self", "❌ You cannot approve your own staff-auth request."))
                .setEphemeral(true)
                .queue();
            case FAIL -> event
                .reply(config.reply("staff-auth-invalid", "❌ Token invalid or already used."))
                .setEphemeral(true)
                .queue();
        }
    }

    private Member resolveMember(ButtonInteractionEvent event, DiscordConfig config) {
        Member member = event.getMember();
        if (member != null) return member;
        if (bot.jda() == null || config == null) return null;

        String guildId = config.resolveGuildId();
        if (guildId == null || guildId.isBlank()) return null;

        Guild guild = bot.jda().getGuildById(guildId);
        if (guild == null) return null;
        return guild.getMember(event.getUser());
    }

    private static boolean mayUseCommands(Member member, String channelId, DiscordConfig config) {
        if (member == null || config == null) return false;

        String restricted = config.commandsChannelId();
        if (restricted != null && !restricted.isBlank() && !restricted.equals(channelId)) return false;

        List<String> roles = config.commandRoleIds();
        if (roles == null || roles.isEmpty()) {
            return member.hasPermission(Permission.MODERATE_MEMBERS) || member.hasPermission(Permission.BAN_MEMBERS);
        }

        Set<String> memberRoles = member.getRoles().stream().map(Role::getId).collect(Collectors.toSet());
        for (String roleId : roles) {
            if (roleId != null && !roleId.isBlank() && memberRoles.contains(roleId.trim())) return true;
        }
        return false;
    }

    private static boolean hasStaffishPermission(Member member) {
        return (
            member != null &&
            (member.hasPermission(Permission.MODERATE_MEMBERS) || member.hasPermission(Permission.ADMINISTRATOR))
        );
    }

    private String replyFor(DiscordLinkService.Redeem redeem, DiscordConfig config, String discordTag) {
        if (redeem == null) return config.reply("sync-unavailable", "❌ Sync unavailable.");

        return switch (redeem.result()) {
            case LINKED -> DiscordEmbeds.apply(
                config.reply("sync-ok", "✅ Linked to **%player%**."),
                Map.of("player", redeem.name() == null ? "?" : redeem.name(), "discord", discordTag)
            );
            case INVALID_CODE -> config.reply("sync-invalid", "❌ Invalid or expired code.");
            case ALREADY_LINKED -> config.reply("sync-already", "❌ Account already linked.");
            case RATE_LIMITED -> config.reply("sync-rate", "❌ Too many attempts. Try again shortly.");
            case UNAVAILABLE -> config.reply("sync-unavailable", "❌ Sync unavailable.");
        };
    }
}
