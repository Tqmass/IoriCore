package us.iorilabs.bukkit.modules.customjoins;

import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public final class CustomJoinsEntry {

    private final String id;
    private final String permission;
    private final boolean joinEnabled;
    private final String joinMessage;
    private final String joinSound;
    private final boolean leaveEnabled;
    private final String leaveMessage;
    private final String leaveSound;

    public CustomJoinsEntry(
        String id,
        String permission,
        boolean joinEnabled,
        String joinMessage,
        String joinSound,
        boolean leaveEnabled,
        String leaveMessage,
        String leaveSound
    ) {
        this.id = id;
        this.permission = permission;
        this.joinEnabled = joinEnabled;
        this.joinMessage = joinMessage == null ? "" : joinMessage;
        this.joinSound = joinSound == null ? "" : joinSound.trim();
        this.leaveEnabled = leaveEnabled;
        this.leaveMessage = leaveMessage == null ? "" : leaveMessage;
        this.leaveSound = leaveSound == null ? "" : leaveSound.trim();
    }
}
