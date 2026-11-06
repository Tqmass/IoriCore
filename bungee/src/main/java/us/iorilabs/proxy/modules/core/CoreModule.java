package us.iorilabs.proxy.modules.core;

import us.iorilabs.proxy.IoriProxy;
import us.iorilabs.proxy.modules.ProxyModule;

public final class CoreModule implements ProxyModule {

    @Override
    public String id() {
        return "core";
    }

    @Override
    public void onEnable(IoriProxy proxy) {
        proxy
            .logger()
            .module(
                id(),
                "bootstrap ready | storage=" + proxy.storage().statusLine() + " | redis=" + proxy.redis().isConnected()
            );
    }
}
