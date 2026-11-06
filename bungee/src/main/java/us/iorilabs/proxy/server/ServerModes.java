package us.iorilabs.proxy.server;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import us.iorilabs.proxy.platform.BackendServer;
import us.iorilabs.proxy.platform.ProxyPlatform;

public final class ServerModes {

    private ServerModes() {}

    public static Map<String, List<String>> resolve(ProxyPlatform platform, Map<String, List<String>> configured) {
        if (configured != null && !configured.isEmpty()) {
            return configured;
        }

        Map<String, List<String>> auto = new LinkedHashMap<>();
        if (platform == null) return auto;

        for (BackendServer server : platform.registeredServers()) {
            String name = server.name();
            if (name == null || name.isBlank()) continue;

            String mode = modeKey(name);
            auto.computeIfAbsent(mode, k -> new ArrayList<>()).add(name);
        }

        return auto;
    }

    public static Optional<String> modeForServer(Map<String, List<String>> modes, String server) {
        if (server == null || server.isBlank() || modes == null || modes.isEmpty()) {
            return Optional.empty();
        }

        for (Map.Entry<String, List<String>> entry : modes.entrySet()) {
            for (String candidate : entry.getValue()) {
                if (candidate.equalsIgnoreCase(server)) {
                    return Optional.of(entry.getKey());
                }
            }
        }

        return Optional.of(modeKey(server));
    }

    public static String modeKey(String serverName) {
        String name = serverName.trim().toLowerCase(Locale.ROOT);
        return name.replaceAll("-\\d+$", "");
    }
}
