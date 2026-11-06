package us.iorilabs.api.messaging;

public enum PluginMessageType {
    HANDSHAKE_PING,
    HANDSHAKE_PONG,
    STAFF_AUTH_SESSION,
    CLIENT_PROTOCOL;

    public byte id() {
        return (byte) ordinal();
    }

    public static PluginMessageType fromId(int id) {
        PluginMessageType[] values = values();
        if (id < 0 || id >= values.length) return null;
        return values[id];
    }
}
