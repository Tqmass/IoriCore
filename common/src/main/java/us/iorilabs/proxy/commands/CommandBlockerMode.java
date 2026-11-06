package us.iorilabs.proxy.commands;

import java.util.Locale;

public enum CommandBlockerMode {
    BLACKLIST,
    WHITELIST;

    public static CommandBlockerMode from(String raw) {
        if (raw == null || raw.trim().isEmpty()) return BLACKLIST;

        return "whitelist".equalsIgnoreCase(raw.trim()) ? WHITELIST : BLACKLIST;
    }

    public String configValue() {
        return name().toLowerCase(Locale.ROOT);
    }
}
