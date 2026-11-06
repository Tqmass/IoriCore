package us.iorilabs.bukkit.modules.gender.command;

import java.util.Collections;
import java.util.Map;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.modules.core.command.EssentialsCommand;
import us.iorilabs.bukkit.modules.gender.GenderKind;
import us.iorilabs.bukkit.modules.gender.GenderService;
import us.iorilabs.proxy.commands.CommandSource;

public final class GenderCommand extends EssentialsCommand {

    private final GenderService service;

    public GenderCommand(IoriCorePlugin plugin, GenderService service) {
        super(plugin, "gender", Collections.emptyList(), "ioricore.gender");
        this.service = service;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (deny(source, "ioricore.gender")) return;
        Player player = requirePlayer(source);
        if (player == null) return;
        if (args.length == 0) {
            plugin.yamlMenus().open(player, "gender");
            return;
        }

        GenderKind kind = GenderKind.parse(args[0]);
        if (kind == GenderKind.UNKNOWN) {
            send(source, "GENDER.INVALID");
            return;
        }
        if (!service.set(player, kind)) {
            send(source, "GENDER.FAILED");
            return;
        }
        send(source, "GENDER.SET", Map.of("gender", service.display(player)));
    }

    @Override
    protected void sendUsage(CommandSource source) {
        send(source, "GENDER.USAGE");
    }
}
