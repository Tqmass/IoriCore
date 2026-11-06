package us.iorilabs.api.hook;

import java.util.UUID;

public final class NoopNLoginHook implements NLoginHook {

    @Override
    public boolean available() {
        return false;
    }

    @Override
    public boolean isAuthenticated(UUID playerId, String playerName) {
        return false;
    }

    @Override
    public boolean isPremium(UUID playerId, String playerName) {
        return false;
    }

    @Override
    public boolean changePassword(UUID playerId, String playerName, String newPassword) {
        return false;
    }
}
