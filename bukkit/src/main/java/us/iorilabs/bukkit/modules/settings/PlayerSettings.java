package us.iorilabs.bukkit.modules.settings;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(fluent = true)
public final class PlayerSettings {

    private final UUID uuid;
    private boolean sounds = true;
    private boolean particles = true;
    private String language;
    private boolean premiumDisplay = true;
    private boolean scoreboard = true;
    private String scoreboardStyle;

    public PlayerSettings(UUID uuid) {
        this.uuid = uuid;
    }
}
