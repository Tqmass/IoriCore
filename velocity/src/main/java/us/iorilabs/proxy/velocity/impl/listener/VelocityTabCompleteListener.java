package us.iorilabs.proxy.velocity.impl.listener;

import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.PlayerAvailableCommandsEvent;
import com.velocitypowered.api.event.player.TabCompleteEvent;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import us.iorilabs.proxy.IoriProxy;
import us.iorilabs.proxy.commands.CommandBlockerService;
import us.iorilabs.proxy.velocity.command.VelocityCommandSource;

public final class VelocityTabCompleteListener {

    private final IoriProxy core;

    public VelocityTabCompleteListener(IoriProxy core) {
        this.core = core;
    }

    @Subscribe
    public void onAvailableCommands(PlayerAvailableCommandsEvent event) {
        if (!enabled()) return;

        VelocityCommandSource source = new VelocityCommandSource(event.getPlayer());
        RootCommandNode<?> root = event.getRootNode();
        List<String> remove = new ArrayList<>();

        for (CommandNode<?> child : root.getChildren()) {
            String name = child.getName();
            String rootName = CommandBlockerService.normalizeRoot(name);
            if (core.commandBlocker().isBlocked(source, rootName)) {
                remove.add(name);
            }
        }

        for (String name : remove) {
            removeRootChild(root, name);
        }
    }

    @Subscribe
    public void onLegacyTabComplete(TabCompleteEvent event) {
        if (!enabled()) return;

        VelocityCommandSource source = new VelocityCommandSource(event.getPlayer());
        String partial = event.getPartialMessage();
        if (partial == null) partial = "";

        String root = CommandBlockerService.normalizeRoot(partial);
        if (root.isBlank()) return;

        if (core.commandBlocker().isBlocked(source, root)) {
            event.getSuggestions().clear();
            return;
        }

        List<String> filtered = core
            .tabCompleteFilter()
            .filter(source, partial, new ArrayList<>(event.getSuggestions()));
        event.getSuggestions().clear();
        event.getSuggestions().addAll(filtered);
    }

    private boolean enabled() {
        return (
            core.tabCompleteFilter() != null &&
            core.commandBlocker() != null &&
            core.configs().settings().moduleEnabled("command_whitelist")
        );
    }

    @SuppressWarnings("unchecked")
    private static void removeRootChild(RootCommandNode<?> root, String name) {
        try {
            Field childrenField = CommandNode.class.getDeclaredField("children");
            childrenField.setAccessible(true);
            Map<String, CommandNode<?>> children = (Map<String, CommandNode<?>>) childrenField.get(root);
            children.remove(name);

            // Also clear literal/argument alias maps when present (Velocity/Brigadier).
            for (String fieldName : List.of("literals", "arguments")) {
                try {
                    Field f = CommandNode.class.getDeclaredField(fieldName);
                    f.setAccessible(true);
                    Object map = f.get(root);
                    if (map instanceof Map<?, ?> m) {
                        m.remove(name);
                    }
                } catch (NoSuchFieldException ignored) {
                    // older brigadier
                }
            }
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Failed to filter command suggestions", ex);
        }
    }
}
