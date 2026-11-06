package us.iorilabs.proxy.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.Getter;

@Getter
public abstract class CommandParent implements ProxyCommand {

    private final String commandName;
    private final List<String> commandAliases;
    private final String commandPermission;
    private final Map<String, CommandArgument> argumentsByName = new LinkedHashMap<>();
    private CommandArgument defaultArgument;

    protected CommandParent(String name, List<String> aliases, String permission) {
        this.commandName = name;
        this.commandAliases =
            aliases == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(aliases));
        this.commandPermission = permission;
    }

    protected void register(CommandArgument argument) {
        argumentsByName.put(argument.name().toLowerCase(Locale.ROOT), argument);
    }

    protected void setDefaultArgument(CommandArgument argument) {
        this.defaultArgument = argument;
    }

    @Override
    public String name() {
        return commandName;
    }

    @Override
    public List<String> aliases() {
        return commandAliases;
    }

    @Override
    public String permission() {
        return commandPermission;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (args.length == 0) {
            if (defaultArgument != null) {
                defaultArgument.execute(source, label, args);
            } else {
                sendUsage(source);
            }

            return;
        }

        CommandArgument argument = argumentsByName.get(args[0].toLowerCase(Locale.ROOT));
        if (argument == null) {
            if (defaultArgument != null) {
                defaultArgument.execute(source, label, args);
            } else {
                sendUsage(source);
            }

            return;
        }

        if (
            argument.permission() != null &&
            !argument.permission().trim().isEmpty() &&
            !source.hasPermission(argument.permission())
        ) {
            source.sendMessage(noPermissionMessage());
            return;
        }

        argument.execute(source, label, copyFrom(args, 1));
    }

    @Override
    public List<String> suggest(CommandSource source, String label, String[] args) {
        if (args.length <= 1) {
            String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
            List<String> suggestions = new ArrayList<>();

            for (CommandArgument argument : argumentsByName.values()) {
                if (argument.name().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    if (
                        argument.permission() == null ||
                        argument.permission().trim().isEmpty() ||
                        source.hasPermission(argument.permission())
                    ) {
                        suggestions.add(argument.name());
                    }
                }
            }

            if (defaultArgument != null) {
                suggestions.addAll(defaultArgument.suggest(source, label, args));
            }

            return suggestions;
        }

        CommandArgument argument = argumentsByName.get(args[0].toLowerCase(Locale.ROOT));
        if (argument != null) return argument.suggest(source, label, copyFrom(args, 1));
        if (defaultArgument != null) return defaultArgument.suggest(source, label, args);

        return Collections.emptyList();
    }

    protected abstract void sendUsage(CommandSource source);

    protected abstract String noPermissionMessage();

    private static String[] copyFrom(String[] args, int from) {
        if (from >= args.length) return new String[0];

        String[] out = new String[args.length - from];
        System.arraycopy(args, from, out, 0, out.length);
        return out;
    }
}
