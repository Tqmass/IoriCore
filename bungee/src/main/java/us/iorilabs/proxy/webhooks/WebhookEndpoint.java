package us.iorilabs.proxy.webhooks;

import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public final class WebhookEndpoint {

    private final String name;
    private final boolean enabled;
    private final String url;
    private final String username;
    private final String avatarUrl;
    private final int color;
    private final String thumbnailUrl;
    private final String imageUrl;

    public WebhookEndpoint(
        String name,
        boolean enabled,
        String url,
        String username,
        String avatarUrl,
        int color,
        String thumbnailUrl,
        String imageUrl
    ) {
        this.name = name;
        this.enabled = enabled;
        this.url = url == null ? "" : url.trim();
        this.username = username == null ? "" : username;
        this.avatarUrl = avatarUrl == null ? "" : avatarUrl;
        this.color = color;
        this.thumbnailUrl = thumbnailUrl == null ? "" : thumbnailUrl;
        this.imageUrl = imageUrl == null ? "" : imageUrl;
    }

    public boolean isConfigured() {
        return enabled && !url.isBlank();
    }

    @Deprecated
    public int defaultColor() {
        return color;
    }
}
