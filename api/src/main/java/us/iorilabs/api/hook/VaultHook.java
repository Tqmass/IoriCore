package us.iorilabs.api.hook;

import java.util.UUID;

public interface VaultHook {
    boolean available();

    String prefix(UUID playerId);

    String suffix(UUID playerId);
}
