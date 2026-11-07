package us.iorilabs.bukkit.modules.lobby.command;

import java.util.Collections;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.modules.core.command.EssentialsCommand;
import us.iorilabs.bukkit.modules.lobby.LobbyCtwMenus;
import us.iorilabs.proxy.commands.CommandSource;

public final class GamesCommand extends EssentialsCommand {

    private final LobbyCtwMenus menus;

    public GamesCommand(IoriCorePlugin plugin, LobbyCtwMenus menus) {
        super(plugin, "games", Collections.emptyList(), "ioricore.games");
        this.menus = menus;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (deny(source, "ioricore.games")) return;
        Player player = requirePlayer(source);
        if (player == null) return;
        menus.openGames(player);
    }

    @Override
    protected void sendUsage(CommandSource source) {}

    @Override
    protected String noPermissionMessage() {
        return language.raw("GENERAL.NO-PERMISSION");
    }
}
