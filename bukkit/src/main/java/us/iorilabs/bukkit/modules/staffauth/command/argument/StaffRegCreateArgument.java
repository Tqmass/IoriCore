package us.iorilabs.bukkit.modules.staffauth.command.argument;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.commands.BukkitCommandSource;
import us.iorilabs.bukkit.modules.staffauth.StaffRegService;
import us.iorilabs.bukkit.modules.staffauth.command.StaffRegCommand;
import us.iorilabs.proxy.commands.CommandArgument;
import us.iorilabs.proxy.commands.CommandSource;

public final class StaffRegCreateArgument extends CommandArgument {

    private final StaffRegCommand parent;
    private final StaffRegService service;

    public StaffRegCreateArgument(StaffRegCommand parent, StaffRegService service) {
        super("create", "royalctw.staffreg.create");
        this.parent = parent;
        this.service = service;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (!(source instanceof BukkitCommandSource bcs) || bcs.playerOrNull() == null) {
            parent.send(source, "GENERAL.PLAYER-ONLY");
            return;
        }
        Player actor = bcs.playerOrNull();
        if (args.length < 3) {
            parent.send(source, "STAFFREG.CREATE-USAGE");
            return;
        }
        String nick = args[1].trim();
        String password = args[2];
        if (!nick.matches("[A-Za-z0-9_]{3,16}")) {
            parent.send(source, "STAFFREG.INVALID-NICK");
            return;
        }
        service.createByAdmin(actor, nick, password, result -> {
            if (result.ok()) {
                parent.send(
                    source,
                    "STAFFREG.CREATED",
                    Map.of("player", result.username(), "uuid", result.uuid().toString())
                );
            } else {
                parent.send(source, result.messageKey());
            }
        });
    }

    @Override
    public List<String> suggest(CommandSource source, String label, String[] args) {
        if (args.length != 2) return List.of();
        String prefix = args[1].toLowerCase(Locale.ROOT);
        return Bukkit.getOnlinePlayers()
            .stream()
            .map(OfflinePlayer::getName)
            .filter(name -> name != null && name.toLowerCase(Locale.ROOT).startsWith(prefix))
            .limit(20)
            .toList();
    }
}
