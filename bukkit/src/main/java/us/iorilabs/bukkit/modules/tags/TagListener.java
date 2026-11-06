package us.iorilabs.bukkit.modules.tags;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class TagListener implements Listener {

    private final TagService service;

    public TagListener(TagService service) {
        this.service = service;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (event.getPlayer() == null) return;
        service.unload(event.getPlayer().getUniqueId());
    }
}
