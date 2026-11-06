package us.iorilabs.bukkit.modules.core.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.modules.core.kit.KitService;
import us.iorilabs.proxy.commands.CommandSource;

public final class DeleteKitCommand extends EssentialsCommand {

    private final KitService kits;

    public DeleteKitCommand(IoriCorePlugin plugin, KitService kits) {
        super(plugin, "deletekit", Collections.singletonList("kitdelete"), "ioricore.essentials.deletekit");
        this.kits = kits;
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (deny(source, "ioricore.essentials.deletekit")) return;
        if (args.length == 0) {
            sendUsage(source);
            return;
        }

        String name = args[0];
        if (!kits.delete(name)) {
            send(source, "ESSENTIALS.DELETEKIT.NOT-FOUND", Map.of("kit", name));
            return;
        }
        send(source, "ESSENTIALS.DELETEKIT.SUCCESS", Map.of("kit", name.toLowerCase()));
    }

    @Override
    public List<String> suggest(CommandSource source, String label, String[] args) {
        if (args.length != 1) return Collections.emptyList();
        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String name : kits.names()) {
            if (name.startsWith(prefix)) out.add(name);
        }
        return out;
    }

    @Override
    protected void sendUsage(CommandSource source) {
        sendUsageLines(source, "ESSENTIALS.DELETEKIT.USAGE");
    }
}
