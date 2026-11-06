package us.iorilabs.proxy.platform;

import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public final class BackendServer {

    private final String name;
    private final boolean online;
    private final int players;

    public BackendServer(String name, boolean online, int players) {
        this.name = name;
        this.online = online;
        this.players = players;
    }
}
