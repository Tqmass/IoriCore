package us.iorilabs.proxy.logging;

import us.iorilabs.proxy.platform.ProxyPlatform;
import us.iorilabs.proxy.utilities.ColorUtil;

public final class ProxyLogger {

    private static final String PREFIX = "&8[&bIoriProxy&8]&r ";

    private final ProxyPlatform platform;

    public ProxyLogger(ProxyPlatform platform) {
        this.platform = platform;
    }

    public void banner(String version, String platformName) {
        platform.info("");
        platform.info(ansi("&b════════════════════════════════════════"));
        platform.info(ansi("&b   ___            _ ____                      "));
        platform.info(ansi("&b  |_ _| ___  _ __(_)  _ \\ _ __ ___ __  ___   "));
        platform.info(ansi("&b   | | / _ \\| '__| | |_) | '__/ _ \\\\ \\/ / | | |"));
        platform.info(ansi("&b   | || (_) | |  | |  __/| | | (_) |>  <| |_| |"));
        platform.info(ansi("&b  |___|\\___/|_|  |_|_|   |_|  \\___/_/\\_\\\\__, |"));
        platform.info(ansi("&b                                        |___/ "));
        platform.info(ansi("&f  IoriProxy &8- &7Proxy Core"));
        platform.info(ansi("&7  v&f" + version + " &8| &7Platform: &f" + platformName));
        platform.info(ansi("&7  Made for &cRoyalCTW"));
        platform.info(ansi("&b════════════════════════════════════════"));
        platform.info("");
    }

    public void info(String message) {
        platform.info(ansi(PREFIX + "&7" + message));
    }

    public void success(String message) {
        platform.info(ansi(PREFIX + "&a" + message));
    }

    public void warn(String message) {
        platform.warn(ansi(PREFIX + "&e" + message));
    }

    public void error(String message) {
        platform.error(ansi(PREFIX + "&c" + message));
    }

    public void error(String message, Throwable throwable) {
        platform.error(ansi(PREFIX + "&c" + message), throwable);
    }

    public void module(String module, String message) {
        platform.info(ansi(PREFIX + "&b[" + module + "] &7" + message));
    }

    private static String ansi(String colored) {
        return ColorUtil.toAnsi(colored);
    }
}
