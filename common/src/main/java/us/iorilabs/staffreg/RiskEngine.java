package us.iorilabs.staffreg;

import java.util.Set;
import us.iorilabs.staffreg.StaffRegEnums.RiskSignal;

public interface RiskEngine {
    Set<RiskSignal> evaluate(TrustSnapshot baseline, TrustSnapshot current, DeviceRecord device, RiskContext context);
}
