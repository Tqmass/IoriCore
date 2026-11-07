package us.iorilabs.api.hook;

import java.util.List;
import java.util.UUID;

public interface ApolloHook {
    boolean available();

    void overrideGlow(UUID targetId, Integer rgb);

    void resetGlow(UUID targetId);

    void setNumbersDisabled(UUID viewerId, boolean disabled);

    void overrideNametag(UUID targetId, List<String> legacyLines, String visibilityOverride);

    void resetNametag(UUID targetId);
}
