package us.iorilabs.bukkit.discord;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

public final class DiscordEmbeds {

    private DiscordEmbeds() {}

    public static MessageEmbed fromTemplate(DiscordConfig.EmbedTemplate template, Map<String, String> placeholders) {
        if (template == null) template = DiscordConfig.EmbedTemplate.EMPTY;
        Map<String, String> ph = placeholders == null ? Map.of() : placeholders;

        EmbedBuilder builder = new EmbedBuilder();
        builder.setColor(parseColor(template.color()));

        String author = apply(template.authorName(), ph);
        String authorIcon = apply(template.authorIconUrl(), ph);
        if (!author.isBlank()) {
            builder.setAuthor(author, null, authorIcon.isBlank() ? null : authorIcon);
        }

        String title = apply(template.title(), ph);
        if (!title.isBlank()) builder.setTitle(title);

        String description = apply(template.description(), ph);
        if (!description.isBlank()) builder.setDescription(description);

        String footer = apply(template.footer(), ph);
        if (!footer.isBlank()) builder.setFooter(footer);

        String thumb = apply(template.thumbnailUrl(), ph);
        if (!thumb.isBlank()) builder.setThumbnail(thumb);

        if (template.timestamp()) builder.setTimestamp(Instant.now());
        return builder.build();
    }

    public static String apply(String template, Map<String, String> placeholders) {
        if (template == null || template.isEmpty()) return "";
        String value = template;
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                String token = entry.getValue() == null ? "" : entry.getValue();
                String key = entry.getKey();
                value = value.replace("%" + key + "%", token);
                value = value.replace("{" + key + "}", token);
                value = value.replace("%" + key.toLowerCase(Locale.ROOT) + "%", token);
                value = value.replace("{" + key.toLowerCase(Locale.ROOT) + "}", token);
                value = value.replace("%" + key.toUpperCase(Locale.ROOT) + "%", token);
                value = value.replace("{" + key.toUpperCase(Locale.ROOT) + "}", token);
            }
        }
        return value;
    }

    public static int parseColor(String raw) {
        if (raw == null || raw.isBlank()) return 0x5865F2;
        String value = raw.trim();
        if (value.startsWith("#")) value = value.substring(1);
        if (value.startsWith("0x") || value.startsWith("0X")) value = value.substring(2);
        try {
            return (int) Long.parseLong(value, 16);
        } catch (NumberFormatException ex) {
            return 0x5865F2;
        }
    }

    public static String visageBust(String player) {
        String name = player == null || player.isBlank() ? "Steve" : player.trim();
        return "https://visage.surgeplay.com/bust/152/" + name;
    }
}
