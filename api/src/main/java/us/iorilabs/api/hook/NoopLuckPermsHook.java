package us.iorilabs.api.hook;

import java.util.UUID;

public final class NoopLuckPermsHook implements LuckPermsHook {

    @Override
    public boolean available() {
        return false;
    }

    @Override
    public void applyPrefix(UUID playerId, String prefix) {}

    @Override
    public String prefix(UUID playerId) {
        return "";
    }

    @Override
    public String suffix(UUID playerId) {
        return "";
    }
}
