package us.iorilabs.bukkit.menu.api.yaml;

import lombok.Getter;
import lombok.experimental.Accessors;
import us.iorilabs.bukkit.menu.enums.MenuActionType;

@Getter
@Accessors(fluent = true)
public final class YamlAction {

    private final MenuActionType type;
    private final String argument;

    public YamlAction(MenuActionType type, String argument) {
        this.type = type;
        this.argument = argument == null ? "" : argument;
    }

    public static YamlAction parse(String raw) {
        if (raw == null) return null;
        String line = raw.trim();
        if (line.isEmpty()) return null;

        String typeToken;
        String argument = "";
        if (line.startsWith("[") && line.contains("]")) {
            int end = line.indexOf(']');
            typeToken = line.substring(1, end).trim();
            argument = end + 1 < line.length() ? line.substring(end + 1).trim() : "";
        } else {
            int space = line.indexOf(' ');
            if (space < 0) {
                typeToken = line;
            } else {
                typeToken = line.substring(0, space).trim();
                argument = line.substring(space + 1).trim();
            }
        }

        MenuActionType type = MenuActionType.parse(typeToken);
        return type == null ? null : new YamlAction(type, argument);
    }
}
