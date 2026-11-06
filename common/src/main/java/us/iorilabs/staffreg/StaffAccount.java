package us.iorilabs.staffreg;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import us.iorilabs.staffreg.StaffRegEnums.AccountStatus;
import us.iorilabs.staffreg.StaffRegEnums.AuthMode;
import us.iorilabs.staffreg.StaffRegEnums.LinkStatus;

@Getter
@Setter
@Accessors(fluent = true)
public final class StaffAccount {

    private UUID uuid;
    private String username;
    private String passwordHash;
    private boolean mustChangePassword;
    private String discordId;
    private LinkStatus linkStatus = LinkStatus.UNLINKED;
    private AccountStatus accountStatus = AccountStatus.ACTIVE;
    private AuthMode preferredAuthMode = AuthMode.DISCORD;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID createdBy;
    private TrustSnapshot trust;
}
