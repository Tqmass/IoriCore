package us.iorilabs.proxy.storage;

import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public final class Migration {

    private final String component;
    private final int version;
    private final String description;
    private final List<MigrationStep> steps;

    public static Migration of(String component, int version, String description, MigrationStep... steps) {
        return new Migration(component, version, description, List.of(steps));
    }
}
