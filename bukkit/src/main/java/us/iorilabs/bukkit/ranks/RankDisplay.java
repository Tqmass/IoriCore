package us.iorilabs.bukkit.ranks;

import java.util.Locale;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.bukkit.ChatColor;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public final class RankDisplay {

    private final String key;
    private final String name;
    private final String display;
    private final String prefix;
    private final String suffix;
    private final String color;
    private final int weight;

    public static RankDisplay of(Rank rank) {
        return new RankDisplay(
            rank.key(),
            rank.name(),
            rank.display(),
            rank.prefix() == null ? "" : rank.prefix(),
            rank.suffix() == null ? "" : rank.suffix(),
            colorCode(rank),
            rank.weight()
        );
    }

    public ChatColor chatColor() {
        if (color.length() < 2 || color.charAt(0) != '&') return null;
        ChatColor value = ChatColor.getByChar(color.charAt(1));
        return value != null && value.isColor() ? value : null;
    }

    private static String colorCode(Rank rank) {
        String raw = rank.colorKey();
        StringBuilder out = new StringBuilder();
        if (raw != null && !raw.trim().isEmpty()) {
            String value = raw.trim();
            if (value.startsWith("&") || value.startsWith("#")) {
                out.append(value);
            } else {
                try {
                    out.append('&').append(ChatColor.valueOf(value.toUpperCase(Locale.ROOT)).getChar());
                } catch (IllegalArgumentException ignored) {
                    out.append("&7");
                }
            }
        }
        if (rank.bold()) out.append("&l");
        if (rank.italic()) out.append("&o");
        return out.toString();
    }
}
