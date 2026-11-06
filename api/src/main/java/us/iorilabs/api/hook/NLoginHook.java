package us.iorilabs.api.hook;

import java.util.UUID;

public interface NLoginHook {
    boolean available();

    boolean isAuthenticated(UUID playerId, String playerName);

    boolean isPremium(UUID playerId, String playerName);

    boolean changePassword(UUID playerId, String playerName, String newPassword);
}
