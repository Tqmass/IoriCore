package us.iorilabs.bukkit.modules.economy;

import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class EconomyVaultBridge {

    private final Object economy;

    private EconomyVaultBridge(Object economy) {
        this.economy = economy;
    }

    public static EconomyVaultBridge tryHook(Logger logger) {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) return null;

        try {
            Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            RegisteredServiceProvider<?> provider = Bukkit.getServicesManager().getRegistration(economyClass);
            if (provider == null || provider.getProvider() == null) return null;

            Object economy = provider.getProvider();
            logger.info("Vault Economy detected (soft-hook)");
            return new EconomyVaultBridge(economy);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    public boolean available() {
        return economy != null;
    }

    public double balance(UUID uuid) {
        if (uuid == null || !available()) return 0.0D;
        try {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
            Object result = economy.getClass().getMethod("getBalance", OfflinePlayer.class).invoke(economy, offline);
            return result instanceof Number ? ((Number) result).doubleValue() : 0.0D;
        } catch (ReflectiveOperationException ex) {
            return 0.0D;
        }
    }

    public boolean deposit(UUID uuid, double amount) {
        if (uuid == null || amount <= 0.0D || !available()) return false;
        return invokeMoney("depositPlayer", uuid, amount);
    }

    public boolean withdraw(UUID uuid, double amount) {
        if (uuid == null || amount <= 0.0D || !available()) return false;
        return invokeMoney("withdrawPlayer", uuid, amount);
    }

    public boolean set(UUID uuid, double amount) {
        if (uuid == null || amount < 0.0D || !available()) return false;
        double current = balance(uuid);
        if (current > amount) return withdraw(uuid, current - amount);
        if (current < amount) return deposit(uuid, amount - current);
        return true;
    }

    public boolean has(UUID uuid, double amount) {
        if (uuid == null || !available()) return false;
        if (amount <= 0.0D) return true;
        try {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
            Object result = economy
                .getClass()
                .getMethod("has", OfflinePlayer.class, double.class)
                .invoke(economy, offline, amount);
            return result instanceof Boolean && (Boolean) result;
        } catch (ReflectiveOperationException ex) {
            return balance(uuid) >= amount;
        }
    }

    private boolean invokeMoney(String method, UUID uuid, double amount) {
        try {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
            Object response = economy
                .getClass()
                .getMethod(method, OfflinePlayer.class, double.class)
                .invoke(economy, offline, amount);
            if (response == null) return false;
            Object ok = response.getClass().getMethod("transactionSuccess").invoke(response);
            return ok instanceof Boolean && (Boolean) ok;
        } catch (ReflectiveOperationException ex) {
            return false;
        }
    }
}
