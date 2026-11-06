package us.iorilabs.bukkit.modules.coinshop.command;

import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.commands.BukkitCommandSource;
import us.iorilabs.bukkit.language.CoreLanguage;
import us.iorilabs.proxy.commands.CommandParent;
import us.iorilabs.proxy.commands.CommandSource;

@Getter
@Accessors(fluent = true)
public final class CoinshopCommand extends CommandParent {

    private final IoriCorePlugin plugin;
    private final CoreLanguage language;

    public CoinshopCommand(IoriCorePlugin plugin) {
        super("coinshop", Arrays.asList("shop", "tienda"), "ioricore.coinshop");
        this.plugin = plugin;
        this.language = plugin.language();
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (!(source instanceof BukkitCommandSource)) {
            source.sendMessage(language.raw(source, "GENERAL.PLAYER-ONLY"));
            return;
        }
        if (!source.hasPermission("ioricore.coinshop") && !source.hasPermission("ioricore.admin")) {
            source.sendMessage(language.raw(source, "GENERAL.NO-PERMISSION"));
            return;
        }

        Player player = ((BukkitCommandSource) source).playerOrNull();
        if (player == null) {
            source.sendMessage(language.raw(source, "GENERAL.PLAYER-ONLY"));
            return;
        }
        plugin.yamlMenus().open(player, "coinshop");
    }

    @Override
    protected void sendUsage(CommandSource source) {
        List<String> lines = language.list(source, "COINSHOP.USAGE");
        for (String line : lines) {
            source.sendMessage(line);
        }
    }

    @Override
    protected String noPermissionMessage() {
        return language.raw("GENERAL.NO-PERMISSION");
    }
}
