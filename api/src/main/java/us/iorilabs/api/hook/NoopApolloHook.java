package us.iorilabs.api.hook;

import java.util.List;
import java.util.UUID;

public final class NoopApolloHook implements ApolloHook {

    @Override
    public boolean available() {
        return false;
    }

    @Override
    public void overrideGlow(UUID targetId, Integer rgb) {}

    @Override
    public void resetGlow(UUID targetId) {}

    @Override
    public void setNumbersDisabled(UUID viewerId, boolean disabled) {}

    @Override
    public void overrideNametag(UUID targetId, List<String> legacyLines, String visibilityOverride) {}

    @Override
    public void resetNametag(UUID targetId) {}
}
