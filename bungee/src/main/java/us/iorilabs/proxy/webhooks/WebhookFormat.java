package us.iorilabs.proxy.webhooks;

import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public final class WebhookFormat {

    private final String content;
    private final int color;
    private final String authorName;
    private final String authorUrl;
    private final String authorIconUrl;
    private final String title;
    private final String titleUrl;
    private final String description;
    private final String footer;
    private final String footerIconUrl;
    private final String thumbnailUrl;
    private final String imageUrl;
    private final boolean timestamp;

    public WebhookFormat(
        String content,
        int color,
        String authorName,
        String authorUrl,
        String authorIconUrl,
        String title,
        String titleUrl,
        String description,
        String footer,
        String footerIconUrl,
        String thumbnailUrl,
        String imageUrl,
        boolean timestamp
    ) {
        this.content = empty(content);
        this.color = color;
        this.authorName = empty(authorName);
        this.authorUrl = empty(authorUrl);
        this.authorIconUrl = empty(authorIconUrl);
        this.title = empty(title);
        this.titleUrl = empty(titleUrl);
        this.description = empty(description);
        this.footer = empty(footer);
        this.footerIconUrl = empty(footerIconUrl);
        this.thumbnailUrl = empty(thumbnailUrl);
        this.imageUrl = empty(imageUrl);
        this.timestamp = timestamp;
    }

    private static String empty(String value) {
        return value == null ? "" : value;
    }
}
