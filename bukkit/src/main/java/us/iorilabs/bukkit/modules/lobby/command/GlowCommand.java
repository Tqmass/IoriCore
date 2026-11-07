package us.iorilabs.bukkit.modules.lobby.command;

import java.util.Collections;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.modules.core.command.EssentialsCommand;
import us.iorilabs.bukkit.modules.lobby.LobbyMenus;
import us.iorilabs.proxy.commands.CommandSource;

public final class GlowCommand extends EssentialsCommand {

    private final LobbyMenus menus;

    public GlowCommand(IoriCorePlugin plugin, LobbyMenus menus) {
        super(plugin, "glow", Collections.emptyList(), "ioricore.glow");
        this.menus = menus;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (deny(source, "ioricore.glow")) return;
        Player player = requirePlayer(source);
        if (player == null) return;
        menus.openGlow(player);
    }

    @Override
    protected void sendUsage(CommandSource source) {}

    @Override
    protected String noPermissionMessage() {
        return language.raw("GENERAL.NO-PERMISSION");
    }
}
