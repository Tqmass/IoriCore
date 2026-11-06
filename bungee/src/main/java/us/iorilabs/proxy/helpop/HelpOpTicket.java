package us.iorilabs.proxy.helpop;

import java.util.UUID;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public final class HelpOpTicket {

    private final String id;
    private final UUID playerUuid;
    private final String playerName;
    private final String message;
    private final String server;
    private final long createdAt;
    private boolean open;

    public HelpOpTicket(
        String id,
        UUID playerUuid,
        String playerName,
        String message,
        String server,
        long createdAt,
        boolean open
    ) {
        this.id = id;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.message = message;
        this.server = server;
        this.createdAt = createdAt;
        this.open = open;
    }

    public void close() {
        this.open = false;
    }
}
