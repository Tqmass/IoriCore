package us.iorilabs.proxy.config;

import java.util.List;
import java.util.Locale;
import lombok.Getter;
import lombok.experimental.Accessors;
import us.iorilabs.staffreg.StaffRegEnums.TrustField;

@Getter
@Accessors(fluent = true)
public final class StaffRegConfig {

    private final String createPermission;
    private final boolean requireStepUpOnCreate;
    private final int passwordMinLength;
    private final boolean mustChangeOnFirstLogin;
    private final boolean requireDiscordLink;
    private final boolean unlinkedNoticeOnJoin;
    private final int unlinkedReminderSeconds;
    private final boolean allowPasswordLoginUnlinked;
    private final int trustExpireDays;
    private final List<TrustField> trustStoreFields;
    private final boolean detectCountry;
    private final boolean detectRegion;
    private final boolean detectCity;
    private final boolean detectVpn;
    private final int challengeTtlSeconds;
    private final int maxFails;
    private final int failWindowSeconds;
    private final String failBanDuration;
    private final boolean stepUpEnabled;
    private final int stepUpTtlSeconds;
    private final List<String> stepUpCommands;
    private final List<String> stepUpPermissions;

    public StaffRegConfig(YamlConfig discordYaml) {
        YamlConfig staffreg = resolveStaffAccount(discordYaml);
        YamlConfig auth = discordYaml.section("AUTH");
        YamlConfig fails = discordYaml.section("FAILS");
        YamlConfig stepUp = discordYaml.section("STEP-UP");

        this.createPermission = staffreg.section("CREATE").getString("PERMISSION", "royalctw.staffreg.create");
        this.requireStepUpOnCreate = staffreg.section("CREATE").getBoolean("REQUIRE-STEP-UP", true);
        this.passwordMinLength = staffreg.section("PASSWORD").getInt("MIN-LENGTH", 12);
        this.mustChangeOnFirstLogin = staffreg.section("PASSWORD").getBoolean("MUST-CHANGE-ON-FIRST-LOGIN", true);
        this.requireDiscordLink = staffreg.getBoolean("REQUIRE-DISCORD-LINK", true);
        this.unlinkedNoticeOnJoin = staffreg.section("UNLINKED").getBoolean("NOTICE-ON-JOIN", true);
        this.unlinkedReminderSeconds = staffreg.section("UNLINKED").getInt("REMINDER-SECONDS", 120);
        this.allowPasswordLoginUnlinked = staffreg.section("UNLINKED").getBoolean("ALLOW-PASSWORD-LOGIN", true);

        this.trustExpireDays = auth.section("TRUST").getInt("EXPIRE-DAYS", 0);
        this.trustStoreFields = parseTrustFields(auth.section("TRUST").getStringList("STORE"));
        this.detectCountry = auth.section("CHANGE-DETECTION").getBoolean("COUNTRY", true);
        this.detectRegion = auth.section("CHANGE-DETECTION").getBoolean("REGION", true);
        this.detectCity = auth.section("CHANGE-DETECTION").getBoolean("CITY", false);
        this.detectVpn = auth.section("CHANGE-DETECTION").getBoolean("VPN-PROXY", true);
        this.challengeTtlSeconds = auth.section("ON-CHANGE").getInt("CHALLENGE-TTL-SECONDS", 120);

        this.maxFails = fails.getInt("MAX-FAILS", 3);
        this.failWindowSeconds = fails.getInt("WINDOW-SECONDS", 900);
        this.failBanDuration = fails.getString("BAN-DURATION", "24h");

        this.stepUpEnabled = stepUp.getBoolean("ENABLED", true);
        this.stepUpTtlSeconds = stepUp.getInt("TTL-SECONDS", 300);
        this.stepUpCommands = stepUp.getStringList("COMMANDS");
        this.stepUpPermissions = stepUp.getStringList("PERMISSION-NODES");
    }

    public boolean hasCreatePermission(String permission) {
        if (permission == null || permission.isBlank()) return false;
        String normalized = permission.toLowerCase(Locale.ROOT);
        return (
            normalized.equals(createPermission.toLowerCase(Locale.ROOT)) ||
            normalized.equals("ioricore.staffreg.create")
        );
    }

    private static YamlConfig resolveStaffAccount(YamlConfig discordYaml) {
        YamlConfig nested = discordYaml.section("STAFF-AUTH").section("ACCOUNT");
        if (nested.contains("CREATE.PERMISSION") || nested.contains("REQUIRE-DISCORD-LINK")) {
            return nested;
        }
        YamlConfig legacy = discordYaml.section("STAFF-AUTH-ACCOUNT");
        if (legacy.contains("CREATE.PERMISSION") || legacy.contains("REQUIRE-DISCORD-LINK")) {
            return legacy;
        }
        return discordYaml.section("STAFFREG");
    }

    private static List<TrustField> parseTrustFields(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of(TrustField.IP, TrustField.COUNTRY, TrustField.REGION);
        }
        return raw
            .stream()
            .map(value -> {
                try {
                    return TrustField.valueOf(value.trim().toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ex) {
                    return null;
                }
            })
            .filter(field -> field != null)
            .toList();
    }
}
