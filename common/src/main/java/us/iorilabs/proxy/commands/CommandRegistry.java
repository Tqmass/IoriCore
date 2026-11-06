package us.iorilabs.proxy.commands;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class CommandRegistry {

    private final Map<String, ProxyCommand> byLabel = new LinkedHashMap<>();

    public void register(ProxyCommand command) {
        byLabel.put(command.name().toLowerCase(Locale.ROOT), command);

        for (String alias : command.aliases()) {
            byLabel.put(alias.toLowerCase(Locale.ROOT), command);
        }
    }

    public void registerConfigured(ProxyCommand command, Collection<String> extraAliases) {
        String primary = command.name().toLowerCase(Locale.ROOT);
        byLabel.put(primary, command);

        if (extraAliases != null) {
            for (String alias : extraAliases) {
                byLabel.put(alias.toLowerCase(Locale.ROOT), command);
            }
        }

        for (String alias : command.aliases()) {
            byLabel.put(alias.toLowerCase(Locale.ROOT), command);
        }
    }

    public Optional<ProxyCommand> find(String label) {
        if (label == null || label.trim().isEmpty()) return Optional.empty();

        return Optional.ofNullable(byLabel.get(label.toLowerCase(Locale.ROOT)));
    }

    public Collection<ProxyCommand> uniqueCommands() {
        return byLabel.values().stream().distinct().toList();
    }

    public List<String> allLabels() {
        return new ArrayList<>(byLabel.keySet());
    }
}
