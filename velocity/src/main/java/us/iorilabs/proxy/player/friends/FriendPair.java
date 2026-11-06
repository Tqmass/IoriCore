package us.iorilabs.proxy.player.friends;

import java.util.UUID;

public record FriendPair(UUID uuidA, UUID uuidB) {
    public static FriendPair ordered(UUID first, UUID second) {
        if (first.compareTo(second) <= 0) return new FriendPair(first, second);

        return new FriendPair(second, first);
    }

    public String memoryKey() {
        return uuidA + ":" + uuidB;
    }
}
