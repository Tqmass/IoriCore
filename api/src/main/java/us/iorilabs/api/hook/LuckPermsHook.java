package us.iorilabs.api.hook;

import java.util.Collection;
import java.util.UUID;

public interface LuckPermsHook {
    boolean available();

    void applyPrefix(UUID playerId, String prefix);

    String prefix(UUID playerId);

    String suffix(UUID playerId);

    default void syncGroup(String groupName, Collection<String> permissions) {}

    default void setUserInherit(UUID playerId, String groupName, boolean add) {}
}
