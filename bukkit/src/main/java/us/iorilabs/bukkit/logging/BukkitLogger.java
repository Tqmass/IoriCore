package us.iorilabs.bukkit.logging;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.plugin.java.JavaPlugin;
import us.iorilabs.proxy.utilities.ColorUtil;

public final class BukkitLogger {

    private static final String PREFIX = "&8[&bIoriCore&8]&r ";

    private final Logger logger;

    public BukkitLogger(JavaPlugin plugin) {
        this.logger = plugin.getLogger();
    }

    public void banner(String version, String platformName) {
        logger.info("");
        logger.info(ansi("&b════════════════════════════════════════"));
        logger.info(ansi("&b   ___            _  ____               "));
        logger.info(ansi("&b  |_ _| ___  _ __(_)/ ___|___  _ __ ___ "));
        logger.info(ansi("&b   | | / _ \\| '__| | |   / _ \\| '__/ _ \\"));
        logger.info(ansi("&b   | || (_) | |  | | |__| (_) | | |  __/"));
        logger.info(ansi("&b  |___|\\___/|_|  |_|\\____\\___/|_|  \\___|"));
        logger.info(ansi("&f  IoriCore &8- &7Bukkit Core"));
        logger.info(ansi("&7  v&f" + version + " &8| &7Platform: &f" + platformName));
        logger.info(ansi("&7  Made for &cRoyalCTW"));
        logger.info(ansi("&b════════════════════════════════════════"));
        logger.info("");
    }

    public void info(String message) {
        logger.info(ansi(PREFIX + "&7" + message));
    }

    public void success(String message) {
        logger.info(ansi(PREFIX + "&a" + message));
    }

    public void warn(String message) {
        logger.warning(ansi(PREFIX + "&e" + message));
    }

    public void error(String message) {
        logger.severe(ansi(PREFIX + "&c" + message));
    }

    public void error(String message, Throwable throwable) {
        logger.log(Level.SEVERE, ansi(PREFIX + "&c" + message), throwable);
    }

    public void module(String module, String message) {
        logger.info(ansi(PREFIX + "&b[" + module + "] &7" + message));
    }

    private static String ansi(String colored) {
        return ColorUtil.toAnsi(colored);
    }
}
