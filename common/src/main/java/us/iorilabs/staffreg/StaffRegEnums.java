package us.iorilabs.staffreg;

public final class StaffRegEnums {

    private StaffRegEnums() {}

    public enum AuthMode {
        DISCORD,
        PASSWORD,
    }

    public enum LinkStatus {
        UNLINKED,
        PENDING_CONFIRM,
        LINKED,
    }

    public enum AccountStatus {
        ACTIVE,
        LOCKED,
        BANNED_SECURITY,
        REVOKED,
    }

    public enum ChallengeKind {
        LOCATION_CHANGE,
        STEP_UP,
        LINK_CONFIRM,
        ADMIN_ASSIGN,
    }

    public enum ChallengeStatus {
        PENDING,
        APPROVED,
        DENIED,
        EXPIRED,
        REPORTED_NOT_ME,
    }

    public enum FailAction {
        NONE,
        LOCK,
        BAN,
    }

    public enum RiskSignal {
        COUNTRY_CHANGE,
        REGION_CHANGE,
        CITY_CHANGE,
        VPN_OR_PROXY,
        TRUST_EXPIRED,
        NO_BASELINE,
    }

    public enum TrustField {
        IP,
        COUNTRY,
        REGION,
        CITY,
    }

    public enum ButtonAction {
        APPROVE,
        DENY,
        NOT_ME,
        CONFIRM_LINK,
        UNBAN,
    }
}
