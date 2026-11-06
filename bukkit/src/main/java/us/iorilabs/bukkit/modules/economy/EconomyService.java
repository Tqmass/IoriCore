package us.iorilabs.bukkit.modules.economy;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.Bukkit;
import us.iorilabs.bukkit.IoriCorePlugin;

@Getter
@Accessors(fluent = true)
public final class EconomyService {

    private final IoriCorePlugin plugin;
    private final EconomyVaultBridge vault;
    private final EconomyYamlStore yaml;
    private final EconomySql sql;
    private final boolean useSql;
    private final boolean persist;
    private final double startingBalance;
    private final Map<UUID, Double> cache = new ConcurrentHashMap<>();
    private final DecimalFormat format;

    public EconomyService(IoriCorePlugin plugin) {
        this.plugin = plugin;
        this.persist = plugin.settings().getBoolean("PERSISTENCE.ECONOMY", true);
        this.startingBalance = plugin.settings().getDouble("ECONOMY.STARTING-BALANCE", 0.0D);
        String storageMode = plugin.settings().getString("ECONOMY.STORAGE", "YAML");
        this.useSql = storageMode != null && storageMode.trim().equalsIgnoreCase("SQL");
        this.vault = EconomyVaultBridge.tryHook(plugin.getLogger());
        this.yaml = new EconomyYamlStore(plugin.getDataFolder(), plugin.getLogger(), persist && !useSql);
        this.sql = new EconomySql(plugin.storage(), plugin.getLogger(), persist && useSql);

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        this.format = new DecimalFormat("#,##0.##", symbols);
    }

    public boolean usingVault() {
        return vault != null && vault.available();
    }

    public double balance(UUID uuid) {
        if (uuid == null) return 0.0D;
        if (usingVault()) {
            double vaultBalance = vault.balance(uuid);
            if (vaultBalance > 0.0D || !cache.containsKey(uuid)) return vaultBalance;
        }
        return internalBalance(uuid);
    }

    private double internalBalance(UUID uuid) {
        Double cached = cache.get(uuid);
        if (cached != null) return cached;

        double loaded = useSql ? sql.load(uuid, startingBalance) : yaml.load(uuid, startingBalance);
        cache.put(uuid, loaded);
        return loaded;
    }

    public boolean deposit(UUID uuid, double amount) {
        if (uuid == null || amount <= 0.0D) return false;
        if (usingVault() && vault.deposit(uuid, amount)) return true;

        double next = internalBalance(uuid) + amount;
        return write(uuid, next);
    }

    public boolean withdraw(UUID uuid, double amount) {
        if (uuid == null || amount <= 0.0D) return false;
        if (usingVault() && vault.withdraw(uuid, amount)) return true;
        if (internalBalance(uuid) < amount) return false;

        return write(uuid, internalBalance(uuid) - amount);
    }

    public boolean set(UUID uuid, double amount) {
        if (uuid == null || amount < 0.0D) return false;
        if (usingVault() && vault.set(uuid, amount)) return true;
        return write(uuid, amount);
    }

    public boolean has(UUID uuid, double amount) {
        if (uuid == null) return false;
        if (amount <= 0.0D) return true;
        if (usingVault()) return vault.has(uuid, amount);
        return balance(uuid) >= amount;
    }

    public boolean pay(UUID from, UUID to, double amount) {
        if (from == null || to == null || amount <= 0.0D) return false;
        if (from.equals(to)) return false;
        if (!has(from, amount)) return false;
        if (!withdraw(from, amount)) return false;
        if (!deposit(to, amount)) {
            deposit(from, amount);
            return false;
        }
        return true;
    }

    public String format(double amount) {
        return format.format(amount) + " " + primaryName();
    }

    public String primaryName() {
        if (plugin.language() == null) return "Royales";
        String lang = plugin.language().language();
        boolean english = lang != null && lang.toLowerCase(Locale.ROOT).startsWith("en");
        String path = english ? "ECONOMY.PRIMARY-NAME.EN" : "ECONOMY.PRIMARY-NAME.ES";
        String value = plugin.language().plainLang(lang, path);
        return value == null || value.isBlank() ? "Royales" : value;
    }

    public String creditsName() {
        if (plugin.language() == null) return "Créditos";
        String lang = plugin.language().language();
        boolean english = lang != null && lang.toLowerCase(Locale.ROOT).startsWith("en");
        String path = english ? "ECONOMY.CREDITS-NAME.EN" : "ECONOMY.CREDITS-NAME.ES";
        String value = plugin.language().plainLang(lang, path);
        return value == null || value.isBlank() ? (english ? "Credits" : "Créditos") : value;
    }

    public String gemsName() {
        if (plugin.language() == null) return "Gemas";
        String lang = plugin.language().language();
        boolean english = lang != null && lang.toLowerCase(Locale.ROOT).startsWith("en");
        String path = english ? "ECONOMY.GEMS-NAME.EN" : "ECONOMY.GEMS-NAME.ES";
        String value = plugin.language().plainLang(lang, path);
        return value == null || value.isBlank() ? (english ? "Gems" : "Gemas") : value;
    }

    public void unload(UUID uuid) {
        if (uuid == null) return;
        cache.remove(uuid);
    }

    private boolean write(UUID uuid, double amount) {
        double safe = Math.max(0.0D, amount);
        cache.put(uuid, safe);
        if (!persist) return true;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (useSql) sql.save(uuid, safe);
            else yaml.save(uuid, safe);
        });
        return true;
    }
}
