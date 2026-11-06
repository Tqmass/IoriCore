package us.iorilabs.bukkit.abstraction;

import us.iorilabs.abstraction.VersionWrapper;

public final class NoopVersionWrapper implements VersionWrapper {

    private final String revision;

    public NoopVersionWrapper(String revision) {
        this.revision = revision;
    }

    @Override
    public String revision() {
        return revision;
    }
}
