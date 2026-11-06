package us.iorilabs.bukkit.modules.chatgames;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import us.iorilabs.bukkit.IoriCorePlugin;

public final class ChatGamesListener implements Listener {

    private final IoriCorePlugin plugin;
    private final ChatGamesService service;

    public ChatGamesListener(IoriCorePlugin plugin, ChatGamesService service) {
        this.plugin = plugin;
        this.service = service;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!service.active().get()) return;
        ChatGameRound round = service.current();
        if (round == null || !round.matches(event.getMessage())) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        String message = event.getMessage();
        Bukkit.getScheduler().runTask(plugin, () -> service.tryAnswer(player, message));
    }
}
