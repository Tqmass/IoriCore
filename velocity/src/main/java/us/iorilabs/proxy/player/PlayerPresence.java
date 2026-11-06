package us.iorilabs.proxy.player;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public final class PlayerPresence {

    private final UUID uuid;
    private final String name;
    private final String server;
    private final String proxyId;
    private final long updatedAt;

    public PlayerPresence(UUID uuid, String name, String server, String proxyId, long updatedAt) {
        this.uuid = Objects.requireNonNull(uuid, "uuid");
        this.name = Objects.requireNonNull(name, "name");
        this.server = server == null ? "" : server;
        this.proxyId = Objects.requireNonNull(proxyId, "proxyId");
        this.updatedAt = updatedAt;
    }

    public String normalizedName() {
        return name.toLowerCase(Locale.ROOT);
    }

    public PlayerPresence withServer(String newServer) {
        return new PlayerPresence(uuid, name, newServer, proxyId, System.currentTimeMillis());
    }
}
