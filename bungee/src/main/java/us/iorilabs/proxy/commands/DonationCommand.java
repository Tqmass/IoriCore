package us.iorilabs.proxy.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import us.iorilabs.proxy.IoriProxy;
import us.iorilabs.proxy.donations.DonationService;

public final class DonationCommand implements ProxyCommand {

    private final IoriProxy proxy;
    private final DonationService donationService;

    public DonationCommand(IoriProxy proxy, DonationService donationService) {
        this.proxy = proxy;
        this.donationService = donationService;
    }

    @Override
    public String name() {
        return "donation";
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

        if (args.length < 1 || !args[0].equalsIgnoreCase("test")) {
            source.sendMessage(proxy.language().raw("donations.test_usage"));
            return;
        }

        if (args.length < 4) {
            source.sendMessage(proxy.language().raw("donations.test_usage"));
            return;
        }

        String player = args[1];
        String pkg = args[2];
        String price = args[3];
        donationService.broadcastDonation("test", player, pkg, price);
        source.sendMessage(proxy.language().raw("donations.test_ok", java.util.Map.of("player", player)));
    }

    @Override
    public List<String> suggest(CommandSource source, String label, String[] args) {
        if (args.length == 1) return List.of("test");

        if (args.length == 2 && args[0].equalsIgnoreCase("test")) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            List<String> out = new ArrayList<>();
            proxy
                .platform()
                .onlinePlayers()
                .forEach(p -> {
                    if (p.name().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                        out.add(p.name());
                    }
                });
            return out;
        }

        return List.of();
    }
}
