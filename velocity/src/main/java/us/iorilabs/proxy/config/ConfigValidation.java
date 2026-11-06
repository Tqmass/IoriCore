package us.iorilabs.proxy.config;

import java.util.ArrayList;
import java.util.List;
import us.iorilabs.proxy.logging.ProxyLogger;

public final class ConfigValidation {

    private final List<String> errors = new ArrayList<>();

    public void error(String file, String key, String message) {
        errors.add(file + " -> " + key + ": " + message);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public void report(ProxyLogger logger) {
        for (String error : errors) {
            logger.error("Config: " + error);
        }
    }

    public List<String> errors() {
        return List.copyOf(errors);
    }
}
