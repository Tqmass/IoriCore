package us.iorilabs.proxy.commands;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public interface ProxyCommand {
    String name();

    default List<String> aliases() {
        return Collections.emptyList();
    }

    String permission();

    void execute(CommandSource source, String label, String[] args);

    default List<String> suggest(CommandSource source, String label, String[] args) {
        return Collections.emptyList();
    }

    default boolean matches(String input) {
        String normalized = input.toLowerCase(Locale.ROOT);
        if (name().equalsIgnoreCase(normalized)) return true;

        for (String alias : aliases()) {
            if (alias.equalsIgnoreCase(normalized)) return true;
        }

        return false;
    }
}
