package us.iorilabs.staffreg;

import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(fluent = true)
public final class DiscordEmbedDto {

    private String templateKey;
    private String color;
    private String authorName;
    private String authorIconUrl;
    private String title;
    private String description;
    private String footer;
    private String thumbnailUrl;
    private boolean timestamp;
    private Map<String, String> placeholders;
}
