package us.iorilabs.bukkit.fx;

public enum SoundKey {
    UI_CLICK,
    TELEPORT,
    SUCCESS,
    ERROR,
    STAFF_ALERT,
    LEVEL_UP,
    PAGE_TURN,
    DENY,
    CONFIRM,
    NOTIFICATION;

    public String configKey() {
        return name();
    }
}
