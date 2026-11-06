package us.iorilabs.staffreg;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import us.iorilabs.staffreg.StaffRegEnums.ButtonAction;

@Getter
@Setter
@Accessors(fluent = true)
public final class DiscordButtonDto {

    private ButtonAction action;
    private String label;
    private String emoji;
    private String style;
    private String signedCustomId;
}
