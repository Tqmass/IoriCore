package us.iorilabs.staffreg;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import us.iorilabs.staffreg.StaffRegEnums.ChallengeKind;
import us.iorilabs.staffreg.StaffRegEnums.ChallengeStatus;

@Getter
@Setter
@Accessors(fluent = true)
public final class Challenge {

    private UUID id;
    private UUID accountUuid;
    private ChallengeKind kind;
    private ChallengeStatus status = ChallengeStatus.PENDING;
    private String tokenHash;
    private String discordMessageId;
    private String channelId;
    private UUID resolvedByUuid;
    private Instant createdAt;
    private Instant expiresAt;
    private Instant resolvedAt;
}
