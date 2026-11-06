package us.iorilabs.proxy.modules;

import us.iorilabs.proxy.IoriProxy;

public interface ProxyModule {
    String id();

    default void onEnable(IoriProxy proxy) {}

    default void onDisable(IoriProxy proxy) {}

    default void onReload(IoriProxy proxy) {}
}
