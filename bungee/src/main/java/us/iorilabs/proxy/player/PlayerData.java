package us.iorilabs.proxy.player;

import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(fluent = true)
public final class PlayerData {

    private final UUID uuid;
    private String name;
    private long firstJoin;
    private long lastJoin;
    private String lastIp;

    public PlayerData(UUID uuid, String name) {
        this.uuid = Objects.requireNonNull(uuid, "uuid");
        this.name = Objects.requireNonNull(name, "name");
    }

    public void touchJoin(String name, String ip, long now) {
        this.name = name;
        this.lastJoin = now;
        if (this.firstJoin <= 0L) {
            this.firstJoin = now;
        }

        if (ip != null && !ip.isBlank()) {
            this.lastIp = ip;
        }
    }
}
