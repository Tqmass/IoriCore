package us.iorilabs.proxy.velocity.impl.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import us.iorilabs.proxy.IoriProxy;
import us.iorilabs.proxy.donations.DonationService;

public final class VelocityDonationChatListener {

    private final IoriProxy core;

    public VelocityDonationChatListener(IoriProxy core) {
        this.core = core;
    }

    @Subscribe
    public void onChat(PlayerChatEvent event) {
        if (!core.configs().settings().moduleEnabled("ggwave")) return;

        DonationService donations = core.donationService();
        if (donations == null || !donations.isGgWaveActive()) return;

        var playerOpt = core.platform().findPlayerByUuid(event.getPlayer().getUniqueId());
        if (playerOpt.isEmpty()) return;

        if (donations.tryGgTrigger(playerOpt.get(), event.getMessage())) {
            event.setResult(PlayerChatEvent.ChatResult.denied());
        }
    }
}
