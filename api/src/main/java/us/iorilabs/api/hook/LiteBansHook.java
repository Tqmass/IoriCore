package us.iorilabs.api.hook;

import java.util.Optional;
import java.util.UUID;

public interface LiteBansHook {
    boolean available();

    boolean isBanned(UUID playerId);

    boolean isMuted(UUID playerId);

    Optional<String> activeBanReason(UUID playerId);
}
