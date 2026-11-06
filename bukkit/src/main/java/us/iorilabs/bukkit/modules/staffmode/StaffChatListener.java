package us.iorilabs.bukkit.modules.staffmode;

import lombok.RequiredArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@RequiredArgsConstructor
public final class StaffChatListener implements Listener {

    private final StaffChatService staffChat;

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        if (staffChat.intercept(event.getPlayer(), event.getMessage())) {
            event.setCancelled(true);
            event.getRecipients().clear();
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        staffChat.untoggle(event.getPlayer().getUniqueId());
    }
}
