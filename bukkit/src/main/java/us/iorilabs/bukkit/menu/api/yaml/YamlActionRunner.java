package us.iorilabs.bukkit.menu.api.yaml;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.fx.SoundService;
import us.iorilabs.bukkit.language.CoreLanguage;
import us.iorilabs.bukkit.menu.enums.MenuActionType;

public final class YamlActionRunner {

    private final YamlMenuService menus;

    public YamlActionRunner(YamlMenuService menus) {
        this.menus = menus;
    }

    public void run(Player player, List<YamlAction> actions, Map<String, String> ctx) {
        if (player == null || actions == null || actions.isEmpty()) return;
        Map<String, String> placeholders = YamlPlaceholders.base(player, ctx);
        for (YamlAction action : actions) {
            if (action == null || action.type() == null) continue;
            if (!dispatch(player, action, placeholders)) return;
        }
    }

    private boolean dispatch(Player player, YamlAction action, Map<String, String> placeholders) {
        String argument = YamlPlaceholders.apply(action.argument(), player, placeholders);
        switch (action.type()) {
            case PLAYER:
                player.performCommand(stripSlash(argument));
                return true;
            case CONSOLE:
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), stripSlash(argument));
                return true;
            case MESSAGE:
                sendMessage(player, argument, placeholders);
                return true;
            case OPEN:
                return openMenu(player, argument, placeholders);
            case BACK:
                menus.navigateBack(player);
                return false;
            case CLOSE:
                player.closeInventory();
                return false;
            case SOUND:
                playSound(player, argument);
                return true;
            case INTERNAL:
                return runInternal(player, argument, placeholders);
            case REFRESH:
                menus.refresh(player);
                return false;
            case PERMISSION:
                if (player.hasPermission(argument) || player.hasPermission("ioricore.admin")) return true;
                CoreLanguage language = menus.plugin().language();
                if (language != null) player.sendMessage(language.rawFor(player, "GENERAL.NO-PERMISSION"));
                return false;
            case PAGE:
                return changePage(player, argument);
            default:
                return true;
        }
    }

    private boolean openMenu(Player player, String argument, Map<String, String> placeholders) {
        String id = YamlPlaceholders.firstToken(argument);
        if (id.isEmpty()) return false;
        Map<String, String> next = new HashMap<>(placeholders);
        String rest = YamlPlaceholders.rest(argument);
        if (!rest.isEmpty()) {
            if (rest.contains("=")) {
                for (String part : rest.split(" ")) {
                    int eq = part.indexOf('=');
                    if (eq <= 0) continue;
                    next.put(part.substring(0, eq).trim(), part.substring(eq + 1).trim());
                }
            } else {
                next.put("id", rest);
                next.put("target", rest);
                next.put("offer", rest);
                next.put("kit", rest);
            }
        }
        menus.open(player, id.toLowerCase(Locale.ROOT), next, true);
        return false;
    }

    private boolean runInternal(Player player, String argument, Map<String, String> placeholders) {
        String id = YamlPlaceholders.firstToken(argument);
        String rest = YamlPlaceholders.rest(argument);
        boolean ok = menus.internals().run(id, player, rest, placeholders, menus);
        if (!ok) {
            menus
                .plugin()
                .getLogger()
                .warning("Unknown menu internal: " + id);
        }
        return true;
    }

    private boolean changePage(Player player, String argument) {
        String token = argument == null ? "" : argument.trim().toLowerCase(Locale.ROOT);
        YamlMenuSession.Session session = menus.sessions().of(player.getUniqueId());
        String menuId = session.currentId();
        if (menuId == null) return false;
        int page = menus.sessions().page(player.getUniqueId(), menuId);
        if ("next".equals(token) || "+".equals(token)) page++;
        else if ("prev".equals(token) || "previous".equals(token) || "-".equals(token)) page = Math.max(0, page - 1);
        else {
            try {
                page = Math.max(0, Integer.parseInt(token));
            } catch (NumberFormatException ignored) {
                return true;
            }
        }
        menus.sessions().page(player.getUniqueId(), menuId, page);
        menus.refresh(player);
        return false;
    }

    private void sendMessage(Player player, String argument, Map<String, String> placeholders) {
        CoreLanguage language = menus.plugin().language();
        if (language == null) {
            player.sendMessage(argument);
            return;
        }
        if (argument.contains(".") && !argument.contains(" ")) {
            player.sendMessage(language.rawFor(player, argument, placeholders));
            return;
        }
        player.sendMessage(us.iorilabs.bukkit.menu.MenuColors.legacy(argument));
    }

    private void playSound(Player player, String argument) {
        IoriCorePlugin plugin = menus.plugin();
        SoundService sounds = plugin.sounds();
        if (sounds == null || argument == null || argument.trim().isEmpty()) return;
        sounds.play(player, argument.trim());
    }

    private static String stripSlash(String command) {
        if (command == null) return "";
        String trimmed = command.trim();
        return trimmed.startsWith("/") ? trimmed.substring(1) : trimmed;
    }
}
