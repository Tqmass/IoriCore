package us.iorilabs.bukkit.modules.staffauth.command.argument;

import java.util.Locale;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import us.iorilabs.bukkit.modules.staffauth.StaffRegService;
import us.iorilabs.bukkit.modules.staffauth.command.StaffRegCommand;
import us.iorilabs.proxy.commands.CommandArgument;
import us.iorilabs.proxy.commands.CommandSource;

public final class StaffRegStatusArgument extends CommandArgument {

    private final StaffRegCommand parent;
    private final StaffRegService service;

    public StaffRegStatusArgument(StaffRegCommand parent, StaffRegService service) {
        super("status", "royalctw.staffreg.create");
        this.parent = parent;
        this.service = service;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (args.length < 2) {
            parent.send(source, "STAFFREG.STATUS-USAGE");
            return;
        }
        String nick = args[1].trim();
        OfflinePlayer target = Bukkit.getOfflinePlayer(nick);
        if (target == null || target.getUniqueId() == null) {
            parent.send(source, "STAFFREG.NOT-FOUND", Map.of("player", nick));
            return;
        }
        boolean registered = service.isRegistered(target.getUniqueId());
        boolean loggedIn = service.isLoggedIn(target.getUniqueId());
        parent.send(
            source,
            "STAFFREG.STATUS",
            Map.of("player", nick, "registered", String.valueOf(registered), "logged_in", String.valueOf(loggedIn))
        );
    }

    @Override
    public java.util.List<String> suggest(CommandSource source, String label, String[] args) {
        if (args.length != 2) return java.util.List.of();
        String prefix = args[1].toLowerCase(Locale.ROOT);
        return Bukkit.getOnlinePlayers()
            .stream()
            .map(p -> p.getName())
            .filter(name -> name != null && name.toLowerCase(Locale.ROOT).startsWith(prefix))
            .limit(20)
            .toList();
    }
}
