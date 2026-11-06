package us.iorilabs.proxy.commands;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import us.iorilabs.proxy.config.CommandWhitelistConfig;

public final class CommandBlockerService {

    private final CommandRegistry registry;
    private CommandWhitelistConfig config;

    public CommandBlockerService(CommandWhitelistConfig config, CommandRegistry registry) {
        this.config = config;
        this.registry = registry;
    }

    public void reload(CommandWhitelistConfig config) {
        this.config = config;
    }

    public boolean isBlocked(CommandSource source, String rootLabel) {
        if (source.hasPermission(config.bypassPermission())) return false;

        String root = normalizeRoot(rootLabel);
        if (root.isBlank()) return false;

        return !isAllowed(source::hasPermission, root);
    }

    public boolean isAllowed(Predicate<String> hasPermission, String root) {
        String normalized = normalizeRoot(root);
        boolean globallyBlocked = config.globalBlocked().contains(normalized);
        Optional<ProxyCommand> owned = registry.find(normalized);

        if (
            config.mode() == CommandBlockerMode.WHITELIST &&
            config.scope() == CommandWhitelistConfig.Scope.PROXY &&
            owned.isEmpty()
        ) {
            return !globallyBlocked;
        }

        Set<String> labels = labelsOf(normalized, owned);
        boolean granted = config
            .groups()
            .stream()
            .filter(group -> group.everyone() || hasPermission.test(group.permission()))
            .anyMatch(group -> labels.stream().anyMatch(group::allows));

        return switch (config.mode()) {
            case WHITELIST -> granted && !globallyBlocked;
            case BLACKLIST -> granted || !globallyBlocked;
        };
    }

    private static Set<String> labelsOf(String label, Optional<ProxyCommand> owned) {
        Set<String> labels = new LinkedHashSet<>();
        labels.add(label);
        owned.ifPresent(command -> labels.add(command.name().toLowerCase(Locale.ROOT)));
        return labels;
    }

    public static String normalizeRoot(String input) {
        if (input == null || input.isBlank()) return "";

        String trimmed = input.trim();
        if (trimmed.startsWith("/")) trimmed = trimmed.substring(1);

        int space = trimmed.indexOf(' ');
        if (space >= 0) trimmed = trimmed.substring(0, space);

        int colon = trimmed.indexOf(':');
        if (colon > 0 && colon < trimmed.length() - 1) {
            trimmed = trimmed.substring(colon + 1);
        }

        return trimmed.toLowerCase(Locale.ROOT);
    }
}
