package us.iorilabs.proxy.commands;

import java.util.ArrayList;
import java.util.List;
import us.iorilabs.proxy.config.CommandWhitelistConfig;
import us.iorilabs.proxy.config.CommandsConfig;

public final class TabCompleteFilterService {

    private final CommandsConfig commands;
    private final CommandRegistry registry;
    private final CommandBlockerService blocker;
    private CommandWhitelistConfig whitelist;

    public TabCompleteFilterService(
        CommandWhitelistConfig whitelist,
        CommandsConfig commands,
        CommandRegistry registry,
        CommandBlockerService blocker
    ) {
        this.whitelist = whitelist;
        this.commands = commands;
        this.registry = registry;
        this.blocker = blocker;
    }

    public void reload(CommandWhitelistConfig whitelist) {
        this.whitelist = whitelist;
    }

    public boolean enabled() {
        return true;
    }

    public List<String> filter(CommandSource source, String partial, List<String> suggestions) {
        if (!enabled() || suggestions == null || suggestions.isEmpty()) {
            return suggestions == null ? List.of() : suggestions;
        }

        if (source.hasPermission(whitelist.bypassPermission())) return suggestions;

        List<String> out = new ArrayList<>(suggestions.size());

        for (String suggestion : suggestions) {
            if (suggestion == null || suggestion.isBlank()) continue;

            String root = rootOf(suggestion, partial);
            if (!blocker.isAllowed(source::hasPermission, root)) continue;
            if (whitelist.hideWithoutPermission() && !maySuggest(source, root)) continue;

            out.add(suggestion);
        }

        return out;
    }

    private boolean maySuggest(CommandSource source, String root) {
        String primary = commands.resolvePrimary(root);
        if (primary != null) return source.hasPermission(commands.permission(primary));

        return registry
            .find(root)
            .map(cmd -> source.hasPermission(cmd.permission()))
            .orElse(true);
    }

    private static String rootOf(String suggestion, String partial) {
        String trimmed = suggestion.trim();
        int space = trimmed.indexOf(' ');
        if (space > 0) return CommandBlockerService.normalizeRoot(trimmed.substring(0, space));

        if (partial != null && partial.contains(" ")) {
            return CommandBlockerService.normalizeRoot(partial.substring(0, partial.indexOf(' ')));
        }

        return CommandBlockerService.normalizeRoot(trimmed);
    }
}
