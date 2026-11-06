package us.iorilabs.bukkit.abstraction;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import us.iorilabs.abstraction.VersionWrapper;

public final class VersionMatcher {

    private static final String EMBEDDED_NMS_PREFIX = "META-INF/ioricore-nms/";
    private static volatile ClassLoader embeddedLoader;

    private VersionMatcher() {}

    public static VersionWrapper load(Logger logger) {
        String revision = detectRevision();
        VersionWrapper wrapper = instantiate(revision);
        if (wrapper == null) {
            wrapper = new NoopVersionWrapper(revision == null ? "unknown" : revision);
            logger.warning("VersionWrapper missing for " + wrapper.revision() + " · using noop");
        } else {
            logger.fine("VersionWrapper · " + wrapper.revision());
        }
        return new FxAwareVersionWrapper(wrapper);
    }

    private static String detectRevision() {
        String pkg = Bukkit.getServer().getClass().getPackage().getName();
        String tail = pkg.substring(pkg.lastIndexOf('.') + 1);
        if (tail.startsWith("v")) return tail;

        String minecraft = Bukkit.getBukkitVersion();
        String ver = minecraft == null ? "" : minecraft.split("-")[0].toLowerCase(Locale.ROOT);
        if (ver.startsWith("26.")) return "v26_R1";
        if (ver.startsWith("1.21")) return "v1_21_R1";
        if (ver.startsWith("1.20")) return "v1_20_R3";
        if (
            ver.startsWith("1.8") ||
            ver.startsWith("1.9") ||
            ver.startsWith("1.10") ||
            ver.startsWith("1.11") ||
            ver.startsWith("1.12")
        ) {
            return "v1_8_R3";
        }
        if (
            ver.startsWith("1.13") ||
            ver.startsWith("1.14") ||
            ver.startsWith("1.15") ||
            ver.startsWith("1.16") ||
            ver.startsWith("1.17") ||
            ver.startsWith("1.18") ||
            ver.startsWith("1.19")
        ) {
            return "v1_20_R3";
        }
        return tail;
    }

    private static VersionWrapper instantiate(String revision) {
        if (revision == null || revision.trim().isEmpty()) return null;
        String className = "us.iorilabs.nms." + revision + ".VersionWrapper_" + revision;
        try {
            Class<?> type = Class.forName(className);
            return (VersionWrapper) type.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException ignored) {}

        try {
            ClassLoader loader = embeddedLoader(revision);
            if (loader == null) return null;
            Class<?> type = Class.forName(className, true, loader);
            return (VersionWrapper) type.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException | IOException ex) {
            return null;
        }
    }

    private static ClassLoader embeddedLoader(String revision) throws IOException {
        ClassLoader existing = embeddedLoader;
        if (existing != null) return existing;

        synchronized (VersionMatcher.class) {
            if (embeddedLoader != null) return embeddedLoader;

            String resource = EMBEDDED_NMS_PREFIX + revision + ".jar";
            InputStream in = VersionMatcher.class.getClassLoader().getResourceAsStream(resource);
            if (in == null) return null;

            Path tmp = Files.createTempFile("ioricore-nms-" + revision + "-", ".jar");
            tmp.toFile().deleteOnExit();
            try (InputStream stream = in) {
                Files.copy(stream, tmp, StandardCopyOption.REPLACE_EXISTING);
            }

            URL[] urls = { tmp.toUri().toURL() };
            embeddedLoader = new URLClassLoader(urls, VersionMatcher.class.getClassLoader());
            return embeddedLoader;
        }
    }
}
