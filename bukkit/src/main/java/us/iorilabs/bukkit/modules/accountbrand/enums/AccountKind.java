package us.iorilabs.bukkit.modules.accountbrand.enums;

public enum AccountKind {
    BEDROCK("ACCOUNT-BRAND.ACCOUNT-BEDROCK"),
    PREMIUM("ACCOUNT-BRAND.ACCOUNT-PREMIUM"),
    CRACKED("ACCOUNT-BRAND.ACCOUNT-CRACKED"),
    UNKNOWN("ACCOUNT-BRAND.ACCOUNT-UNKNOWN");

    private final String messageKey;

    AccountKind(String messageKey) {
        this.messageKey = messageKey;
    }

    public String messageKey() {
        return messageKey;
    }
}
