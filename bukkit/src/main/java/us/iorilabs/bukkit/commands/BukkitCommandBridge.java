package us.iorilabs.bukkit.commands;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.language.CoreLanguage;
import us.iorilabs.proxy.commands.ProxyCommand;

public final class BukkitCommandBridge extends Command {

    private static final String ADMIN = "ioricore.admin";

    private final JavaPlugin plugin;
    private final ProxyCommand command;
    private final String node;

    private BukkitCommandBridge(JavaPlugin plugin, ProxyCommand command, String node, List<String> aliases) {
        super(command.name());
        this.plugin = plugin;
        this.command = command;
        this.node = node;
        setAliases(aliases);
        if (node != null) setPermission(node);
        PluginCommand declared = plugin.getCommand(command.name());
        if (declared != null && declared.getDescription() != null) setDescription(declared.getDescription());
    }

    public static void register(JavaPlugin plugin, ProxyCommand command) {
        String key = command.name().toUpperCase(Locale.ROOT);
        FileConfiguration commands = plugin instanceof IoriCorePlugin ? ((IoriCorePlugin) plugin).commands() : null;
        FileConfiguration permissions =
            plugin instanceof IoriCorePlugin ? ((IoriCorePlugin) plugin).permissions() : null;

        CommandMap map = commandMap();
        Map<String, Command> known = knownCommands(map);
        if (map == null || known == null) {
            plugin.getLogger().warning("Command map unavailable · /" + command.name() + " not registered");
            return;
        }
        release(plugin, known, command.name());

        boolean enabled = commands == null || commands.getBoolean("COMMANDS." + key + ".ENABLED", true);
        if (!enabled) {
            plugin.getLogger().info("Command disabled in commands.yml · /" + command.name());
            return;
        }

        List<String> aliases =
            commands != null && commands.isList("COMMANDS." + key + ".ALIASES")
                ? commands.getStringList("COMMANDS." + key + ".ALIASES")
                : command.aliases();
        String node = blankToNull(
            permissions == null ? command.permission() : permissions.getString("COMMANDS." + key, command.permission())
        );

        BukkitCommandBridge bridge = new BukkitCommandBridge(plugin, command, node, cleanAliases(aliases));
        bridge.setPermissionMessage("");
        String fallback = plugin.getName().toLowerCase(Locale.ROOT);
        map.register(fallback, bridge);
        claim(known, fallback, bridge.getName(), bridge);
        for (String alias : bridge.getAliases()) {
            claim(known, fallback, alias, bridge);
        }
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!allowed(sender)) {
            sender.sendMessage(language().raw(sender, "GENERAL.NO-PERMISSION"));
            return true;
        }
        command.execute(source(sender), label, args == null ? new String[0] : args);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        if (!allowed(sender)) return Collections.emptyList();
        List<String> suggestions = command.suggest(source(sender), alias, args == null ? new String[0] : args);
        return suggestions == null ? Collections.<String>emptyList() : suggestions;
    }

    @Override
    public boolean testPermissionSilent(CommandSender target) {
        return allowed(target);
    }

    @Override
    public boolean testPermission(CommandSender target) {
        if (testPermissionSilent(target)) return true;
        if (target != null) target.sendMessage(language().raw(target, "GENERAL.NO-PERMISSION"));
        return false;
    }

    private boolean allowed(CommandSender sender) {
        return node == null || sender.hasPermission(node) || sender.hasPermission(ADMIN);
    }

    private BukkitCommandSource source(CommandSender sender) {
        String original = blankToNull(command.permission());
        if (original == null || node == null || original.equals(node)) return new BukkitCommandSource(sender);
        return new BukkitCommandSource(sender, original, node);
    }

    private CoreLanguage language() {
        return ((IoriCorePlugin) plugin).language();
    }

    private static void claim(Map<String, Command> known, String fallback, String label, Command command) {
        String key = label.toLowerCase(Locale.ROOT);
        known.put(key, command);
        known.put(fallback + ":" + key, command);
    }

    private static void release(JavaPlugin plugin, Map<String, Command> known, String name) {
        Iterator<Map.Entry<String, Command>> it = known.entrySet().iterator();
        while (it.hasNext()) {
            Command value = it.next().getValue();
            boolean ours =
                (value instanceof BukkitCommandBridge &&
                    ((BukkitCommandBridge) value).plugin == plugin &&
                    value.getName().equalsIgnoreCase(name)) ||
                (value instanceof PluginCommand &&
                    ((PluginCommand) value).getPlugin() == plugin &&
                    value.getName().equalsIgnoreCase(name));
            if (ours) it.remove();
        }
    }

    private static List<String> cleanAliases(List<String> aliases) {
        if (aliases == null || aliases.isEmpty()) return new ArrayList<>();
        List<String> out = new ArrayList<>(aliases.size());
        for (String alias : aliases) {
            if (alias == null || alias.trim().isEmpty()) continue;
            out.add(alias.trim().toLowerCase(Locale.ROOT));
        }
        return out;
    }

    private static String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private static CommandMap commandMap() {
        try {
            Field field = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            field.setAccessible(true);
            return (CommandMap) field.get(Bukkit.getServer());
        } catch (ReflectiveOperationException | ClassCastException ex) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Command> knownCommands(CommandMap map) {
        if (map == null) return null;
        Class<?> type = map.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField("knownCommands");
                field.setAccessible(true);
                return (Map<String, Command>) field.get(map);
            } catch (NoSuchFieldException ex) {
                type = type.getSuperclass();
            } catch (ReflectiveOperationException | ClassCastException ex) {
                return null;
            }
        }
        return null;
    }

    public static void syncCommands() {
        try {
            Method sync = Bukkit.getServer().getClass().getDeclaredMethod("syncCommands");
            sync.setAccessible(true);
            sync.invoke(Bukkit.getServer());
        } catch (ReflectiveOperationException | LinkageError ignored) {}
    }
}
