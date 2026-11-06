package us.iorilabs.proxy.logging;

public final class LibraryLogSilence {

    private static final String[] LOGGERS = {
        "us.iorilabs.proxy.libs.hikari",
        "us.iorilabs.proxy.libs.hikari.HikariDataSource",
        "us.iorilabs.proxy.libs.hikari.pool",
        "us.iorilabs.proxy.libs.hikari.pool.HikariPool",
        "us.iorilabs.proxy.libs.h2",
        "us.iorilabs.proxy.libs.jedis",
        "com.zaxxer.hikari",
        "org.h2",
        "redis.clients.jedis",
    };

    private LibraryLogSilence() {}

    public static void apply() {
        silenceLog4j();
        silenceJul();
    }

    private static void silenceLog4j() {
        try {
            Class<?> levelClass = Class.forName("org.apache.logging.log4j.Level");
            Object warn = levelClass.getField("WARN").get(null);
            Class<?> configurator = Class.forName("org.apache.logging.log4j.core.config.Configurator");
            var setLevel = configurator.getMethod("setLevel", String.class, levelClass);

            for (String name : LOGGERS) {
                setLevel.invoke(null, name, warn);
            }
        } catch (ReflectiveOperationException ignored) {
            // Velocity always has Log4j2; if not, skip.
        }
    }

    private static void silenceJul() {
        for (String name : LOGGERS) {
            java.util.logging.Logger.getLogger(name).setLevel(java.util.logging.Level.WARNING);
        }
    }
}
