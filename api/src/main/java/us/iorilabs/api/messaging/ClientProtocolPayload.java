package us.iorilabs.api.messaging;

public final class ClientProtocolPayload {

    public static final int LENGTH = 5;

    private ClientProtocolPayload() {}

    public static byte[] encode(int protocol) {
        return new byte[] {
            PluginMessageType.CLIENT_PROTOCOL.id(),
            (byte) (protocol >>> 24),
            (byte) (protocol >>> 16),
            (byte) (protocol >>> 8),
            (byte) protocol,
        };
    }

    public static int decode(byte[] message) {
        if (message == null || message.length != LENGTH) return -1;
        if (message[0] != PluginMessageType.CLIENT_PROTOCOL.id()) return -1;
        return (
            ((message[1] & 0xFF) << 24) | ((message[2] & 0xFF) << 16) | ((message[3] & 0xFF) << 8) | (message[4] & 0xFF)
        );
    }
}
