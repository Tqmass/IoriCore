package us.iorilabs.bukkit.modules.settings.command;

import java.util.Collections;
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
public final class SettingsCommand extends CommandParent {

    private final IoriCorePlugin plugin;
    private final CoreLanguage language;

    public SettingsCommand(IoriCorePlugin plugin, CoreLanguage language) {
        super("settings", Collections.singletonList("preferencias"), "ioricore.settings");
        this.plugin = plugin;
        this.language = language;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (!(source instanceof BukkitCommandSource)) {
            source.sendMessage(language.raw(source, "GENERAL.PLAYER-ONLY"));
            return;
        }
        Player player = ((BukkitCommandSource) source).playerOrNull();
        if (player == null) {
            source.sendMessage(language.raw(source, "GENERAL.PLAYER-ONLY"));
            return;
        }
        plugin.yamlMenus().open(player, "settings");
    }

    @Override
    protected void sendUsage(CommandSource source) {
        List<String> lines = language.list(source, "SETTINGS.USAGE");
        for (String line : lines) {
            source.sendMessage(line);
        }
    }

    @Override
    protected String noPermissionMessage() {
        return language.raw("GENERAL.NO-PERMISSION");
    }
}
