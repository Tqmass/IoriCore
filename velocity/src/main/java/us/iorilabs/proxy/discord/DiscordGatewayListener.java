package us.iorilabs.proxy.discord;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateBoostTimeEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import us.iorilabs.proxy.config.DiscordConfig;
import us.iorilabs.proxy.discord.enums.DiscordChannelKey;
import us.iorilabs.proxy.discord.enums.DiscordPunishmentType;

public final class DiscordGatewayListener extends ListenerAdapter {

    private final DiscordBotService bot;

    public DiscordGatewayListener(DiscordBotService bot) {
        this.bot = bot;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot() || !event.isFromGuild()) return;

        DiscordConfig config = bot.config();
        if (config == null || !config.enabled()) return;

        String staffChatId = config.channel(DiscordChannelKey.STAFF_CHAT.id());
        if (!staffChatId.isBlank() && staffChatId.equals(event.getChannel().getId())) {
            String content = event.getMessage().getContentDisplay();
            if (content != null && !content.isBlank()) {
                bot.relayDiscordStaffChat(
                    event.getAuthor().getName(),
                    event.getAuthor().getEffectiveAvatarUrl(),
                    content
                );
            }
            return;
        }

        if (!config.prefixCommands()) return;

        String raw = event.getMessage().getContentRaw();
        String prefix = config.prefix();
        if (prefix == null || prefix.isBlank() || !raw.startsWith(prefix)) return;

        String body = raw.substring(prefix.length()).trim();
        if (body.isBlank()) return;

        String[] parts = body.split("\\s+");
        String label = parts[0].toLowerCase(Locale.ROOT);
        DiscordPunishmentType type = DiscordPunishmentType.fromName(label);
        if (type == null || !config.commandEnabled(label)) return;

        if (!mayUseCommands(event.getMember(), event.getChannel().getId(), config)) {
            event.getChannel().sendMessage("❌ Missing Discord role or wrong channel.").queue();
            return;
        }

        String player;
        String duration = null;
        String reason;
        int index = 1;
        if (parts.length <= index) {
            event
                .getChannel()
                .sendMessage(
                    "Usage: `" +
                        prefix +
                        label +
                        " <player> " +
                        (type.requiresDuration() ? "<duration> " : "") +
                        "[reason]`"
                )
                .queue();
            return;
        }

        player = parts[index++];
        if (type.requiresDuration()) {
            if (parts.length <= index) {
                event.getChannel().sendMessage("Missing duration (e.g. `1h`, `30m`).").queue();
                return;
            }
            duration = parts[index++];
        }

        reason =
            parts.length > index
                ? String.join(" ", java.util.Arrays.copyOfRange(parts, index, parts.length))
                : "No reason provided";
        bot.handlePunishment(type, player, duration, reason, event.getAuthor().getName(), event.getChannel());
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        DiscordConfig config = bot.config();
        if (config == null || !config.enabled() || !config.slashCommands()) return;

        if ("status".equalsIgnoreCase(event.getName())) {
            String server = option(event, "server");
            event.deferReply(true).queue();
            bot.sendStatusReply(event.getHook(), server);
            return;
        }

        DiscordPunishmentType type = DiscordPunishmentType.fromName(event.getName());
        if (type == null || !config.commandEnabled(event.getName())) {
            event.reply("Command disabled.").setEphemeral(true).queue();
            return;
        }

        if (!mayUseCommands(event.getMember(), event.getChannel() == null ? "" : event.getChannel().getId(), config)) {
            event.reply("❌ Missing Discord role or wrong channel.").setEphemeral(true).queue();
            return;
        }

        String player = option(event, "player");
        String duration = option(event, "duration");
        String reason = option(event, "reason");
        if (reason == null || reason.isBlank()) reason = "No reason provided";

        event.deferReply(true).queue();
        bot.handlePunishment(type, player, duration, reason, event.getUser().getName(), event.getHook());
    }

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        if (!"status".equalsIgnoreCase(event.getName())) return;
        if (!"server".equalsIgnoreCase(event.getFocusedOption().getName())) return;

        DiscordConfig config = bot.config();
        if (config == null || !config.enabled() || !config.status().enabled()) return;

        event.replyChoiceStrings(bot.statusChoices(event.getFocusedOption().getValue())).queue();
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        bot.onGuildMemberJoin(event.getMember());
    }

    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
        bot.onGuildMemberRemove(event.getMember());
    }

    @Override
    public void onGuildMemberUpdateBoostTime(GuildMemberUpdateBoostTimeEvent event) {
        if (event.getNewTimeBoosted() == null) return;
        Member member = event.getMember();
        var guild = member.getGuild();
        bot.emitBoost(
            member.getEffectiveName(),
            member.getUser().getEffectiveAvatarUrl(),
            guild.getBoostCount(),
            guild.getName()
        );
    }

    private static boolean mayUseCommands(Member member, String channelId, DiscordConfig config) {
        if (member == null) return false;

        String restricted = config.commandsChannelId();
        if (restricted != null && !restricted.isBlank() && !restricted.equals(channelId)) return false;

        List<String> roles = config.commandRoleIds();
        if (roles == null || roles.isEmpty()) {
            return member.hasPermission(Permission.MODERATE_MEMBERS) || member.hasPermission(Permission.BAN_MEMBERS);
        }

        Set<String> memberRoles = member
            .getRoles()
            .stream()
            .map(Role::getId)
            .collect(java.util.stream.Collectors.toSet());
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

    private static String option(SlashCommandInteractionEvent event, String name) {
        OptionMapping mapping = event.getOption(name);
        return mapping == null ? null : mapping.getAsString();
    }
}
