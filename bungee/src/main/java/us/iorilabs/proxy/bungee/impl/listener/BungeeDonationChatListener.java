package us.iorilabs.proxy.bungee.impl.listener;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import us.iorilabs.proxy.IoriProxy;
import us.iorilabs.proxy.donations.DonationService;

public final class BungeeDonationChatListener implements Listener {

    private final IoriProxy core;

    public BungeeDonationChatListener(IoriProxy core) {
        this.core = core;
    }

    @EventHandler
    public void onChat(ChatEvent event) {
        if (!(event.getSender() instanceof ProxiedPlayer handle)) return;
        if (!core.configs().settings().moduleEnabled("ggwave")) return;

        DonationService donations = core.donationService();
        if (donations == null || !donations.isGgWaveActive()) return;

        String message = event.getMessage();
        if (message.startsWith("/")) return;

        var playerOpt = core.platform().findPlayerByUuid(handle.getUniqueId());
        if (playerOpt.isEmpty()) return;

        if (donations.tryGgTrigger(playerOpt.get(), message)) {
            event.setCancelled(true);
        }
    }
}
