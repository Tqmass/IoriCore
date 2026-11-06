package us.iorilabs.proxy.commands;

import us.iorilabs.proxy.IoriProxy;
import us.iorilabs.proxy.donations.DonationService;

public final class GgWaveCommand implements ProxyCommand {

    private final IoriProxy proxy;
    private final DonationService donationService;

    public GgWaveCommand(IoriProxy proxy, DonationService donationService) {
        this.proxy = proxy;
        this.donationService = donationService;
    }

    @Override
    public String name() {
        return "ggwave";
    }

    @Override
    public String permission() {
        return proxy.configs().commands().permission("donations");
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        if (!donationService.enabled()) {
            source.sendMessage(proxy.language().raw("general.feature_disabled"));
            return;
        }

        donationService.startGgWaveManual();
        source.sendMessage(proxy.language().raw("donations.ggwave_started"));
    }
}
