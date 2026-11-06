package us.iorilabs.bukkit.modules.tags.command;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.entity.Player;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.language.CoreLanguage;
import us.iorilabs.bukkit.modules.core.EssentialsPlayers;
import us.iorilabs.bukkit.modules.tags.Tag;
import us.iorilabs.bukkit.modules.tags.TagService;
import us.iorilabs.bukkit.modules.tags.command.argument.TagEquipArgument;
import us.iorilabs.bukkit.modules.tags.command.argument.TagHelpArgument;
import us.iorilabs.bukkit.modules.tags.command.argument.TagListArgument;
import us.iorilabs.bukkit.modules.tags.command.argument.TagUnequipArgument;
import us.iorilabs.proxy.commands.CommandParent;
import us.iorilabs.proxy.commands.CommandSource;

@Getter
@Accessors(fluent = true)
public final class TagCommand extends CommandParent {

    private final IoriCorePlugin plugin;
    private final CoreLanguage language;
    private final TagService tags;

    public TagCommand(IoriCorePlugin plugin, TagService tags) {
        super("tag", Arrays.asList("tags"), "ioricore.tags");
        this.plugin = plugin;
        this.language = plugin.language();
        this.tags = tags;

        TagListArgument list = new TagListArgument(this);
        register(list);
        register(new TagEquipArgument(this));
        register(new TagUnequipArgument(this));
        register(new TagHelpArgument(this));
        setDefaultArgument(list);
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (!source.hasPermission("ioricore.tags") && !source.hasPermission("ioricore.admin")) {
            source.sendMessage(language.raw(source, "GENERAL.NO-PERMISSION"));
            return;
        }
        super.execute(source, label, args);
    }

    public void list(CommandSource source) {
        Player player = EssentialsPlayers.player(source);
        if (player == null) {
            source.sendMessage(language.raw(source, "GENERAL.PLAYER-ONLY"));
            return;
        }

        List<Tag> visible = tags.visibleFor(player);
        if (visible.isEmpty()) {
            source.sendMessage(language.raw(source, "TAGS.NONE"));
            return;
        }

        Tag equipped = tags.equipped(player.getUniqueId());
        source.sendMessage(language.raw(source, "TAGS.LIST-HEADER"));
        for (Tag tag : visible) {
            boolean active = equipped != null && equipped.id().equals(tag.id());
            source.sendMessage(
                language.raw(
                    source,
                    active ? "TAGS.LIST-LINE-ACTIVE" : "TAGS.LIST-LINE",
                    Map.of("id", tag.id(), "display", tag.display())
                )
            );
        }
        if (equipped == null) {
            source.sendMessage(language.raw(source, "TAGS.EQUIPPED-NONE"));
        } else {
            source.sendMessage(
                language.raw(source, "TAGS.EQUIPPED", Map.of("id", equipped.id(), "display", equipped.display()))
            );
        }
    }

    public void equip(CommandSource source, String id) {
        Player player = EssentialsPlayers.player(source);
        if (player == null) {
            source.sendMessage(language.raw(source, "GENERAL.PLAYER-ONLY"));
            return;
        }
        if (id == null || id.trim().isEmpty()) {
            sendUsage(source);
            return;
        }

        Tag tag = tags.get(id);
        if (tag == null) {
            source.sendMessage(language.raw(source, "TAGS.UNKNOWN", Map.of("id", id)));
            return;
        }
        if (!tags.canUse(player, tag)) {
            source.sendMessage(language.raw(source, "GENERAL.NO-PERMISSION"));
            return;
        }
        if (!tags.equip(player, tag.id())) {
            source.sendMessage(language.raw(source, "TAGS.EQUIP-FAILED"));
            return;
        }
        source.sendMessage(language.raw(source, "TAGS.EQUIPPED-SET", Map.of("id", tag.id(), "display", tag.display())));
    }

    public void unequip(CommandSource source) {
        Player player = EssentialsPlayers.player(source);
        if (player == null) {
            source.sendMessage(language.raw(source, "GENERAL.PLAYER-ONLY"));
            return;
        }
        if (!tags.unequip(player.getUniqueId())) {
            source.sendMessage(language.raw(source, "TAGS.EQUIPPED-NONE"));
            return;
        }
        source.sendMessage(language.raw(source, "TAGS.UNEQUIPPED"));
    }

    @Override
    public void sendUsage(CommandSource source) {
        List<String> lines = language.list(source, "TAGS.USAGE");
        for (String line : lines) {
            source.sendMessage(line);
        }
    }

    @Override
    protected String noPermissionMessage() {
        return language.raw("GENERAL.NO-PERMISSION");
    }
}
