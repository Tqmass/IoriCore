package us.iorilabs.proxy.config;

import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public final class StaffAuthConfig {

    private final boolean maskIp;
    private final boolean geoLookup;
    private final String geoUrlTemplate;
    private final String staffPermissionFallback;
    private final int tokenTtlSeconds;
    private final int graceSeconds;
    private final boolean webhookEnable;

    public StaffAuthConfig(YamlConfig discordYaml) {
        YamlConfig root = discordYaml.section("STAFF-AUTH");
        YamlConfig yaml = root.section("GEO");
        if (!yaml.contains("MASK-IP") && !yaml.contains("STAFF-PERMISSION")) {
            yaml = root;
        }
        this.maskIp = yaml.getBoolean("MASK-IP", true);
        this.geoLookup = yaml.getBoolean("GEO-LOOKUP", true);
        this.geoUrlTemplate = yaml.getString(
            "GEO-URL",
            "http://ip-api.com/json/%ip%?fields=status,country,countryCode"
        );
        this.staffPermissionFallback = yaml.getString("STAFF-PERMISSION", "ioriproxy.staff");
        this.tokenTtlSeconds = Math.max(60, yaml.getInt("TOKEN-TTL-SECONDS", 900));
        this.graceSeconds = Math.max(0, yaml.getInt("GRACE-SECONDS", 2));
        this.webhookEnable = yaml.getBoolean("WEBHOOK-ENABLE", true);
    }
}
