package us.iorilabs.proxy.commands;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public abstract class CommandArgument {

    private final String name;
    private final String permission;

    protected CommandArgument(String name, String permission) {
        this.name = name;
        this.permission = permission;
    }

    public boolean matches(String token) {
        return name.equalsIgnoreCase(token);
    }

    public abstract void execute(CommandSource source, String label, String[] args);

    public List<String> suggest(CommandSource source, String label, String[] args) {
        return Collections.emptyList();
    }

    protected String firstArg(String[] args, String def) {
        return args.length == 0 ? def : args[0];
    }

    protected String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
