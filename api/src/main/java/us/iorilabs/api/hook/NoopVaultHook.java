package us.iorilabs.api.hook;

import java.util.UUID;

public final class NoopVaultHook implements VaultHook {

    @Override
    public boolean available() {
        return false;
    }

    @Override
    public String prefix(UUID playerId) {
        return "";
    }

    @Override
    public String suffix(UUID playerId) {
        return "";
    }
}
