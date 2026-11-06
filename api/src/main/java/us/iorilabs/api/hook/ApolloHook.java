package us.iorilabs.api.hook;

import java.util.UUID;

public interface ApolloHook {
    boolean available();

    void overrideGlow(UUID targetId, Integer rgb);

    void resetGlow(UUID targetId);

    void setNumbersDisabled(UUID viewerId, boolean disabled);
}
