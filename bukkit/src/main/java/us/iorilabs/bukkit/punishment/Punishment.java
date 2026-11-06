package us.iorilabs.bukkit.punishment;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import us.iorilabs.api.punishment.PunishmentType;
import us.iorilabs.bukkit.punishment.enums.PunishmentStatus;

@Getter
@Setter
@Accessors(fluent = true)
public final class Punishment {

    private long id;
    private PunishmentType type = PunishmentType.WARN;
    private PunishmentStatus status = PunishmentStatus.ACTIVE;
    private UUID targetId;
    private String targetName;
    private UUID actorId;
    private String actorName;
    private String reason;
    private long createdAt;
    private long expiresAt;
    private boolean silent;
}
