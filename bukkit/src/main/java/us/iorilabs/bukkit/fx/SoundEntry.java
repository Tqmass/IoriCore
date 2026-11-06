package us.iorilabs.bukkit.fx;

import java.util.Collections;
import java.util.List;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public final class SoundEntry {

    private final String key;
    private final List<String> names;
    private final float volume;
    private final float pitch;

    public SoundEntry(String key, List<String> names, float volume, float pitch) {
        this.key = key;
        this.names = names == null ? Collections.<String>emptyList() : names;
        this.volume = volume;
        this.pitch = pitch;
    }
}
