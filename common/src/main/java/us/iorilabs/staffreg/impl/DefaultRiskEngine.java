package us.iorilabs.staffreg.impl;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import us.iorilabs.staffreg.DeviceRecord;
import us.iorilabs.staffreg.RiskContext;
import us.iorilabs.staffreg.RiskEngine;
import us.iorilabs.staffreg.StaffRegEnums.RiskSignal;
import us.iorilabs.staffreg.StaffRegEnums.TrustField;
import us.iorilabs.staffreg.TrustSnapshot;

public final class DefaultRiskEngine implements RiskEngine {

    @Override
    public Set<RiskSignal> evaluate(
        TrustSnapshot baseline,
        TrustSnapshot current,
        DeviceRecord device,
        RiskContext context
    ) {
        if (context == null) return Set.of(RiskSignal.NO_BASELINE);
        if (baseline == null) return Set.of(RiskSignal.NO_BASELINE);

        EnumSet<RiskSignal> signals = EnumSet.noneOf(RiskSignal.class);
        TrustSnapshot observed = current != null ? current : snapshotFromDevice(device);
        if (observed == null) return Set.of(RiskSignal.NO_BASELINE);

        if (baseline.expiresAt() != null && baseline.expiresAt().isBefore(Instant.now())) {
            signals.add(RiskSignal.TRUST_EXPIRED);
        }

        if (context.treatVpnAsChange() && observed.vpnOrProxy() && !baseline.vpnOrProxy()) {
            signals.add(RiskSignal.VPN_OR_PROXY);
        }

        if (context.detectCountry() && context.storedFields().contains(TrustField.COUNTRY)) {
            if (!equalsIgnoreCase(baseline.country(), observed.country())) signals.add(RiskSignal.COUNTRY_CHANGE);
        }

        if (context.detectRegion() && context.storedFields().contains(TrustField.REGION)) {
            if (!equalsIgnoreCase(baseline.region(), observed.region())) signals.add(RiskSignal.REGION_CHANGE);
        }

        if (context.detectCity() && context.storedFields().contains(TrustField.CITY)) {
            if (!equalsIgnoreCase(baseline.city(), observed.city())) signals.add(RiskSignal.CITY_CHANGE);
        }

        return signals.isEmpty() ? Set.of() : Set.copyOf(signals);
    }

    private static TrustSnapshot snapshotFromDevice(DeviceRecord device) {
        if (device == null) return null;

        return new TrustSnapshot()
            .ipHmac(device.ipHmac())
            .country(device.country())
            .region(device.region())
            .city(device.city())
            .vpnOrProxy(device.vpnOrProxy());
    }

    private static boolean equalsIgnoreCase(String left, String right) {
        if (left == null || left.isBlank()) return right == null || right.isBlank();
        if (right == null || right.isBlank()) return false;
        return left.equalsIgnoreCase(right);
    }
}
