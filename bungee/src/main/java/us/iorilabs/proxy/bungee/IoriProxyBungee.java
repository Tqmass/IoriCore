package us.iorilabs.proxy.bungee;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.md_5.bungee.api.plugin.Plugin;
import us.iorilabs.proxy.IoriProxy;
import us.iorilabs.proxy.bungee.impl.listener.BungeeCommandBlockerListener;
import us.iorilabs.proxy.bungee.impl.listener.BungeeDonationChatListener;
import us.iorilabs.proxy.bungee.impl.listener.BungeePluginMessageListener;
import us.iorilabs.proxy.bungee.impl.listener.BungeePresenceListener;
import us.iorilabs.proxy.bungee.impl.listener.BungeeStaffChatListener;
import us.iorilabs.proxy.bungee.impl.listener.BungeeTabCompleteListener;

@Accessors(fluent = true)
public final class IoriProxyBungee extends Plugin {

    @Getter
    private IoriProxy core;

    private BungeePlatform platform;
    private BungeePluginMessageListener pluginMessaging;

    @Override
    public void onEnable() {
        platform = new BungeePlatform(this);
        core = new IoriProxy(platform);
        core.enable();
        getProxy()
            .getPluginManager()
            .registerListener(this, new BungeePresenceListener(this, core, getProxy(), platform.audiences()));
        getProxy().getPluginManager().registerListener(this, new BungeeCommandBlockerListener(core, platform));
        getProxy().getPluginManager().registerListener(this, new BungeeStaffChatListener(core));
        getProxy().getPluginManager().registerListener(this, new BungeeTabCompleteListener(core, platform));
        getProxy().getPluginManager().registerListener(this, new BungeeDonationChatListener(core));

        pluginMessaging = new BungeePluginMessageListener(core, getProxy(), platform.audiences());
        getProxy().getPluginManager().registerListener(this, pluginMessaging);
        pluginMessaging.enable();

        if (core.configs().settings().moduleEnabled("server_health")) {
            us.iorilabs.proxy.server.ServerHealthMonitor health = new us.iorilabs.proxy.server.ServerHealthMonitor(
                platform,
                core.language(),
                core.logger(),
                core.configs().commands().permission("server_health"),
                (name, onOk, onFail) -> {
                    net.md_5.bungee.api.config.ServerInfo info = getProxy().getServerInfo(name);
                    if (info == null) {
                        onFail.run();
                        return;
                    }

                    info.ping((result, error) -> {
                        if (error != null || result == null) {
                            onFail.run();
                        } else {
                            onOk.run();
                        }
                    });
                }
            );
            health.start();
        }
    }

    @Override
    public void onDisable() {
        if (pluginMessaging != null) {
            pluginMessaging.disable();
            pluginMessaging = null;
        }

        if (core != null) {
            core.disable();
            core = null;
        }

        if (platform != null) {
            platform.close();
            platform = null;
        }
    }
}
