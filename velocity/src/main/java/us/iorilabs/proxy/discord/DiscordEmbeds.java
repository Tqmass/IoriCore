package us.iorilabs.proxy.discord;

import java.awt.Color;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import us.iorilabs.proxy.config.DiscordConfig;
import us.iorilabs.proxy.webhooks.WebhookClient;
import us.iorilabs.proxy.webhooks.WebhookFormat;
import us.iorilabs.proxy.webhooks.WebhookRegistry;

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

    public static MessageEmbed fromWebhookFormat(WebhookFormat format, Map<String, String> placeholders) {
        if (format == null) return fromTemplate(DiscordConfig.EmbedTemplate.EMPTY, placeholders);
        Map<String, String> ph = placeholders == null ? Map.of() : placeholders;

        EmbedBuilder builder = new EmbedBuilder();
        builder.setColor(format.color() == 0 ? 0x5865F2 : format.color());

        String author = apply(format.authorName(), ph);
        String authorIcon = apply(format.authorIconUrl(), ph);
        if (!author.isBlank()) {
            builder.setAuthor(author, null, authorIcon.isBlank() ? null : authorIcon);
        }

        String title = apply(format.title(), ph);
        if (!title.isBlank()) {
            String titleUrl = apply(format.titleUrl(), ph);
            if (titleUrl.isBlank()) builder.setTitle(title);
            else builder.setTitle(title, titleUrl);
        }

        String description = apply(format.description(), ph);
        if (!description.isBlank()) builder.setDescription(description);

        String footer = apply(format.footer(), ph);
        String footerIcon = apply(format.footerIconUrl(), ph);
        if (!footer.isBlank()) builder.setFooter(footer, footerIcon.isBlank() ? null : footerIcon);

        String thumb = apply(format.thumbnailUrl(), ph);
        if (!thumb.isBlank()) builder.setThumbnail(thumb);

        String image = apply(format.imageUrl(), ph);
        if (!image.isBlank()) builder.setImage(image);

        if (format.timestamp()) builder.setTimestamp(Instant.now());
        return builder.build();
    }

    public static String apply(String template, Map<String, String> placeholders) {
        return WebhookClient.apply(template, placeholders);
    }

    public static int parseColor(String raw) {
        return WebhookRegistry.parseColor(raw, 0x5865F2);
    }

    public static Color awtColor(String raw) {
        return new Color(parseColor(raw));
    }

    public static String visageBust(String player) {
        String name = player == null || player.isBlank() ? "Steve" : player.trim();
        return "https://visage.surgeplay.com/bust/152/" + name;
    }

    public static String visageFace(String player) {
        String name = player == null || player.isBlank() ? "Steve" : player.trim();
        return "https://visage.surgeplay.com/face/" + name;
    }

    public static String normalizeKey(String key) {
        if (key == null) return "";
        return key.toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
