package us.iorilabs.bukkit.modules.tags;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import us.iorilabs.api.modules.ModuleId;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.commands.BukkitCommandBridge;
import us.iorilabs.bukkit.modules.CoreModule;
import us.iorilabs.bukkit.modules.coinshop.CoinshopModule;
import us.iorilabs.bukkit.modules.tags.command.TagCommand;

@Getter
@Accessors(fluent = true)
public final class TagsModule implements CoreModule {

    private final IoriCorePlugin plugin;
    private final boolean enabledFlag;
    private TagService service;
    private TagListener listener;

    public TagsModule(IoriCorePlugin plugin) {
        this.plugin = plugin;
        this.enabledFlag = plugin.settings().getBoolean("MODULES.TAGS", true);
    }

    @Override
    public ModuleId id() {
        return ModuleId.TAGS;
    }

    @Override
    public boolean enabled() {
        return enabledFlag;
    }

    @Override
    public void enable() {
        us.iorilabs.bukkit.util.PluginResources.saveDefault(plugin, "tags.yml");
        service = new TagService(plugin);
        service.load();
        listener = new TagListener(service);
        Bukkit.getPluginManager().registerEvents(listener, plugin);
        BukkitCommandBridge.register(plugin, new TagCommand(plugin, service));
    }

    @Override
    public void disable() {
        if (listener != null) {
            HandlerList.unregisterAll(listener);
            listener = null;
        }
        service = null;
    }

    @Override
    public void reload() {
        if (service != null) service.load();
        CoinshopModule shop = plugin.modules() == null ? null : plugin.modules().get(ModuleId.COINSHOP);
        if (shop != null && shop.enabled() && shop.service() != null) shop.service().load();
    }
}
