package us.iorilabs.api.hook;

import java.util.Optional;
import java.util.UUID;

public final class NoopLiteBansHook implements LiteBansHook {

    @Override
    public boolean available() {
        return false;
    }

    @Override
    public boolean isBanned(UUID playerId) {
        return false;
    }

    @Override
    public boolean isMuted(UUID playerId) {
        return false;
    }

    @Override
    public Optional<String> activeBanReason(UUID playerId) {
        return Optional.empty();
    }
}
