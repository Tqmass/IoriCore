package us.iorilabs.bukkit.hook;

import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import us.iorilabs.api.hook.VaultHook;

public final class SoftVaultHook implements VaultHook {

    private final boolean present;
    private Object chat;

    public SoftVaultHook(Logger logger, boolean moduleEnabled) {
        if (!moduleEnabled) {
            this.present = false;
            return;
        }

        this.present = Bukkit.getPluginManager().getPlugin("Vault") != null;
        if (present) {
            logger.fine("Vault detected (soft-hook)");
            tryLoadChat();
        }
    }

    private void tryLoadChat() {
        try {
            Class<?> chatClass = Class.forName("net.milkbowl.vault.chat.Chat");
            RegisteredServiceProvider<?> provider = Bukkit.getServicesManager().getRegistration(chatClass);
            if (provider != null) chat = provider.getProvider();
        } catch (ReflectiveOperationException ignored) {
            chat = null;
        }
    }

    @Override
    public boolean available() {
        return present && chat != null;
    }

    @Override
    public String prefix(UUID playerId) {
        return invokeChat("getPlayerPrefix", playerId);
    }

    @Override
    public String suffix(UUID playerId) {
        return invokeChat("getPlayerSuffix", playerId);
    }

    private String invokeChat(String method, UUID playerId) {
        if (playerId == null || !available()) return "";
        try {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(playerId);
            Object result = chat
                .getClass()
                .getMethod(method, String.class, OfflinePlayer.class)
                .invoke(chat, "", offline);
            return result == null ? "" : String.valueOf(result);
        } catch (ReflectiveOperationException ex) {
            try {
                OfflinePlayer offline = Bukkit.getOfflinePlayer(playerId);
                Object result = chat.getClass().getMethod(method, OfflinePlayer.class).invoke(chat, offline);
                return result == null ? "" : String.valueOf(result);
            } catch (ReflectiveOperationException ignored) {
                return "";
            }
        }
    }
}
