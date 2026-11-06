package us.iorilabs.bukkit.modules.core.command;

import java.util.Collections;
import java.util.Map;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.modules.core.kit.KitService;
import us.iorilabs.proxy.commands.CommandSource;

public final class CreateKitCommand extends EssentialsCommand {

    private final KitService kits;

    public CreateKitCommand(IoriCorePlugin plugin, KitService kits) {
        super(plugin, "createkit", Collections.singletonList("kitcreate"), "ioricore.essentials.createkit");
        this.kits = kits;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (deny(source, "ioricore.essentials.createkit")) return;
        Player player = requirePlayer(source);
        if (player == null) return;
        if (args.length == 0) {
            sendUsage(source);
            return;
        }

        String name = args[0];
        if (kits.get(name) != null) {
            send(source, "ESSENTIALS.CREATEKIT.EXISTS", Map.of("kit", name));
            return;
        }
        if (!kits.create(name, player)) {
            send(source, "ESSENTIALS.CREATEKIT.EMPTY");
            return;
        }
        send(source, "ESSENTIALS.CREATEKIT.SUCCESS", Map.of("kit", name.toLowerCase()));
    }

    @Override
    public java.util.List<String> suggest(CommandSource source, String label, String[] args) {
        return Collections.emptyList();
    }

    @Override
    protected void sendUsage(CommandSource source) {
        sendUsageLines(source, "ESSENTIALS.CREATEKIT.USAGE");
    }
}
