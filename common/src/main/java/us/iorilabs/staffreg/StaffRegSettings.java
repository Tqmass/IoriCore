package us.iorilabs.staffreg;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import us.iorilabs.staffreg.StaffRegEnums.AuthMode;
import us.iorilabs.staffreg.StaffRegEnums.FailAction;
import us.iorilabs.staffreg.StaffRegEnums.TrustField;

@Getter
@Setter
@Accessors(fluent = true)
public final class StaffRegSettings {

    private String createPermission = "royalctw.staffreg.create";
    private boolean createRequireStepUp = true;
    private int passwordMinLength = 12;
    private boolean mustChangeOnFirstLogin = true;
    private boolean requireDiscordLink = true;
    private boolean oneDiscordPerAccount = true;
    private boolean adminAssignEnabled = true;
    private boolean adminAssignRequireOwnerConfirm = true;
    private int adminAssignConfirmTtlSeconds = 300;
    private boolean unlinkedNoticeOnJoin = true;
    private int unlinkedReminderSeconds = 120;
    private boolean unlinkedAllowPasswordLogin = true;
    private AuthMode unlinkedStepUpFallback = AuthMode.PASSWORD;
    private Set<AuthMode> authModes = EnumSet.of(AuthMode.DISCORD, AuthMode.PASSWORD);
    private int trustExpireDays;
    private Set<TrustField> trustStore = EnumSet.of(
        TrustField.IP,
        TrustField.COUNTRY,
        TrustField.REGION,
        TrustField.CITY
    );
    private boolean detectCountry = true;
    private boolean detectRegion = true;
    private boolean detectCity;
    private boolean detectVpnProxy = true;
    private boolean onChangeStripPermissions = true;
    private boolean onChangeKeepRankTag = true;
    private boolean onChangeBlockStaffCommands = true;
    private int onChangeChallengeTtlSeconds = 120;
    private int onChangeKickAfterSeconds;
    private boolean onChangeAllowPasswordFallback = true;
    private boolean enforceOpAndWildcard = true;
    private int failsMax = 3;
    private int failsWindowSeconds = 900;
    private FailAction failsAction = FailAction.BAN;
    private String failsBanDuration = "24h";
    private boolean failsRevokeSessions = true;
    private boolean failsAlertDiscord = true;
    private boolean failsUnbanButton = true;
    private boolean stepUpEnabled = true;
    private int stepUpTtlSeconds = 300;
    private AuthMode stepUpMethod = AuthMode.DISCORD;
    private boolean stepUpConsole = true;
    private boolean stepUpPermanentBan = true;
    private List<String> stepUpCommands = List.of(
        "op",
        "deop",
        "lp",
        "luckperms",
        "stop",
        "restart",
        "reload",
        "staffreg"
    );
    private List<String> stepUpPermissionNodes = List.of("*", "minecraft.command.op");
    private int syncCodeTtlSeconds = 300;
    private String bridgeSecretEnv = "ROYALCTW_BRIDGE_SECRET";
    private String ipPepperEnv = "ROYALCTW_STAFFREG_PEPPER";
}
