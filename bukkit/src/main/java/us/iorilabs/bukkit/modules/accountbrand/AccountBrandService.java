package us.iorilabs.bukkit.modules.accountbrand;

import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import us.iorilabs.api.hook.GeyserHook;
import us.iorilabs.api.hook.NLoginHook;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.modules.accountbrand.enums.AccountKind;

@RequiredArgsConstructor
public final class AccountBrandService {

    private final IoriCorePlugin plugin;
    private final boolean geyserHook;
    private final boolean nLoginHook;

    public AccountKind resolveKind(Player player) {
        if (player == null) return AccountKind.UNKNOWN;

        if (geyserHook) {
            GeyserHook geyser = plugin.geyserHook();
            if (geyser != null && geyser.available() && geyser.isBedrock(player.getUniqueId())) {
                return AccountKind.BEDROCK;
            }
        }

        if (nLoginHook) {
            NLoginHook nLogin = plugin.nLoginHook();
            if (nLogin != null && nLogin.available()) {
                return nLogin.isPremium(player.getUniqueId(), player.getName())
                    ? AccountKind.PREMIUM
                    : AccountKind.CRACKED;
            }
        }

        if (Bukkit.getOnlineMode()) return AccountKind.PREMIUM;

        return AccountKind.UNKNOWN;
    }
}
