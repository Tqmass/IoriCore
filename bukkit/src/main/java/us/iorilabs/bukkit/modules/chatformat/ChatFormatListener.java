package us.iorilabs.bukkit.modules.chatformat;

import java.util.HashSet;
import java.util.Set;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public final class ChatFormatListener implements Listener {

    private final ChatFormatService service;

    public ChatFormatListener(ChatFormatService service) {
        this.service = service;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled()) return;

        Set<Player> recipients = new HashSet<>(event.getRecipients());
        event.setCancelled(true);
        event.getRecipients().clear();

        Player sender = event.getPlayer();
        String message = event.getMessage();
        service.broadcast(sender, message, recipients);
    }
}
