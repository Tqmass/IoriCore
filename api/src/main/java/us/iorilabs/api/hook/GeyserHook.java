package us.iorilabs.api.hook;

import java.util.UUID;

public interface GeyserHook {
    boolean available();

    boolean isBedrock(UUID playerId);
}
