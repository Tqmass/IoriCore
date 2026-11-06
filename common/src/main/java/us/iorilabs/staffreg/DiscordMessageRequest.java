package us.iorilabs.staffreg;

import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(fluent = true)
public final class DiscordMessageRequest {

    private String channelId;
    private String discordUserId;
    private UUID accountUuid;
    private DiscordEmbedDto embed;
    private List<DiscordButtonDto> buttons;
    private boolean ephemeral;
}
