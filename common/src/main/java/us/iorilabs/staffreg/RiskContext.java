package us.iorilabs.staffreg;

import java.util.EnumSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import us.iorilabs.staffreg.StaffRegEnums.TrustField;

@Getter
@Setter
@Accessors(fluent = true)
public final class RiskContext {

    private boolean detectCountry = true;
    private boolean detectRegion = true;
    private boolean detectCity;
    private boolean treatVpnAsChange = true;
    private Set<TrustField> storedFields = EnumSet.of(TrustField.IP, TrustField.COUNTRY, TrustField.REGION);
}
