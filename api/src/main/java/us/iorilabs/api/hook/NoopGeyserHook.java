package us.iorilabs.api.hook;

import java.util.UUID;

public final class NoopGeyserHook implements GeyserHook {

    @Override
    public boolean available() {
        return false;
    }

    @Override
    public boolean isBedrock(UUID playerId) {
        return false;
    }
}
