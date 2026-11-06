package us.iorilabs.staffreg.bridge;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(fluent = true)
public final class DiscordBridgeEvent {

    private String eventId = UUID.randomUUID().toString();
    private BridgeEventType type;
    private long timestamp = System.currentTimeMillis();
    private String signature;
    private Map<String, String> fields = new LinkedHashMap<>();

    public DiscordBridgeEvent put(String key, String value) {
        if (key != null && value != null) fields.put(key, value);
        return this;
    }

    public String field(String key) {
        return fields.get(key);
    }

    public String canonicalPayload() {
        StringBuilder sb = new StringBuilder();
        sb.append(eventId)
            .append('|')
            .append(type == null ? "" : type.name())
            .append('|')
            .append(timestamp);
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            sb.append('|').append(entry.getKey()).append('=').append(entry.getValue());
        }
        return sb.toString();
    }
}
