package us.iorilabs.bukkit.modules.chatgames.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.Getter;
import lombok.experimental.Accessors;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.language.CoreLanguage;
import us.iorilabs.bukkit.modules.chatgames.ChatGamesService;
import us.iorilabs.bukkit.modules.chatgames.enums.ChatGameType;
import us.iorilabs.proxy.commands.CommandParent;
import us.iorilabs.proxy.commands.CommandSource;

@Getter
@Accessors(fluent = true)
public final class ChatGamesCommand extends CommandParent {

    private final IoriCorePlugin plugin;
    private final CoreLanguage language;
    private final ChatGamesService service;

    public ChatGamesCommand(IoriCorePlugin plugin, ChatGamesService service) {
        super("chatgames", Arrays.asList("cg", "chatgame"), "ioricore.chatgames");
        this.plugin = plugin;
        this.language = plugin.language();
        this.service = service;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (!source.hasPermission("ioricore.chatgames") && !source.hasPermission("ioricore.admin")) {
            source.sendMessage(language.raw(source, "GENERAL.NO-PERMISSION"));
            return;
        }
        if (args.length == 0) {
            sendUsage(source);
            return;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("start")) {
            if (!source.hasPermission("ioricore.chatgames.admin") && !source.hasPermission("ioricore.admin")) {
                source.sendMessage(language.raw(source, "GENERAL.NO-PERMISSION"));
                return;
            }
            ChatGameType forced = args.length >= 2 ? ChatGameType.byId(args[1]) : null;
            if (args.length >= 2 && forced == null) {
                source.sendMessage(language.raw(source, "CHAT-GAMES.UNKNOWN-TYPE", Map.of("type", args[1])));
                return;
            }
            if (!service.tryStartGame(forced)) {
                source.sendMessage(language.raw(source, "CHAT-GAMES.START-FAILED"));
                return;
            }
            source.sendMessage(language.raw(source, "CHAT-GAMES.START-FORCED"));
            return;
        }
        if (sub.equals("skip") || sub.equals("end")) {
            if (!source.hasPermission("ioricore.chatgames.admin") && !source.hasPermission("ioricore.admin")) {
                source.sendMessage(language.raw(source, "GENERAL.NO-PERMISSION"));
                return;
            }
            service.endRound(true);
            source.sendMessage(language.raw(source, "CHAT-GAMES.SKIPPED"));
            return;
        }
        sendUsage(source);
    }

    @Override
    public List<String> suggest(CommandSource source, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("start", "skip", "help");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("start")) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            List<String> out = new ArrayList<>();
            for (ChatGameType type : ChatGameType.values()) {
                if (type.id().startsWith(prefix)) out.add(type.id());
            }
            return out;
        }
        return Collections.emptyList();
    }

    @Override
    protected void sendUsage(CommandSource source) {
        List<String> lines = language.list(source, "CHAT-GAMES.USAGE");
        for (String line : lines) {
            source.sendMessage(line);
        }
    }

    @Override
    protected String noPermissionMessage() {
        return language.raw("GENERAL.NO-PERMISSION");
    }
}
