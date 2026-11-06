package us.iorilabs.bukkit.modules.chatgames.enums;

import java.util.Locale;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public enum ChatGameType {
    TRIVIA("trivia"),
    REACTION("reaction"),
    UNSCRAMBLE("unscramble"),
    MATH("math"),
    ALGEBRA("algebra");

    private final String id;

    ChatGameType(String id) {
        this.id = id;
    }

    public String messageKey() {
        return name();
    }

    public static ChatGameType byId(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        String needle = raw.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        for (ChatGameType type : values()) {
            if (type.id.equals(needle) || type.name().equalsIgnoreCase(raw.trim())) return type;
        }
        return null;
    }
}
