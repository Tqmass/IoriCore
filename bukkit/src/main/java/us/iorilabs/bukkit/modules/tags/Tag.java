package us.iorilabs.bukkit.modules.tags;

import java.util.List;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public final class Tag {

    private final String id;
    private final String display;
    private final String permission;
    private final String emoji;
    private final String chat;
    private final List<String> hover;
    private final double shopPrice;
    private final String shopMaterial;
    private final short shopData;

    public Tag(
        String id,
        String display,
        String permission,
        String emoji,
        String chat,
        List<String> hover,
        double shopPrice,
        String shopMaterial,
        short shopData
    ) {
        this.id = id;
        this.display = display == null ? id : display;
        this.permission = permission == null ? "" : permission;
        this.emoji = emoji == null ? "" : emoji;
        this.chat = chat == null || chat.isEmpty() ? resolveFallback(this.emoji, this.display) : chat;
        this.hover = hover == null || hover.isEmpty() ? List.of() : List.copyOf(hover);
        this.shopPrice = shopPrice;
        this.shopMaterial = shopMaterial == null || shopMaterial.isBlank() ? "" : shopMaterial.trim();
        this.shopData = shopData;
    }

    public List<String> hoverLines() {
        return hover;
    }

    public String fragment() {
        if (chat != null && !chat.isEmpty()) return chat;
        if (emoji != null && !emoji.isEmpty()) return emoji;
        return display == null ? "" : display;
    }

    private static String resolveFallback(String emoji, String display) {
        if (emoji != null && !emoji.isEmpty()) return emoji + " ";
        return display == null ? "" : display;
    }
}
