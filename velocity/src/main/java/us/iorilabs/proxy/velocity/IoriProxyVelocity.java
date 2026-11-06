package us.iorilabs.proxy.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import java.nio.file.Path;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.slf4j.Logger;
import us.iorilabs.proxy.IoriProxy;
import us.iorilabs.proxy.velocity.impl.listener.VelocityCommandBlockerListener;
import us.iorilabs.proxy.velocity.impl.listener.VelocityDonationChatListener;
import us.iorilabs.proxy.velocity.impl.listener.VelocityPluginMessageListener;
import us.iorilabs.proxy.velocity.impl.listener.VelocityPresenceListener;
import us.iorilabs.proxy.velocity.impl.listener.VelocityStaffChatListener;
import us.iorilabs.proxy.velocity.impl.listener.VelocityStaffRegListener;
import us.iorilabs.proxy.velocity.impl.listener.VelocityTabCompleteListener;

@Plugin(
    id = "iorivelo",
    name = "IoriVelo",
    version = "1.1.5",
    description = "IoriVelo — IoriLabs Velocity proxy core",
    authors = { "Tqmass" },
    dependencies = { @Dependency(id = "luckperms", optional = true) }
)
@Accessors(fluent = true)
public final class IoriProxyVelocity {

    @Getter
    private final ProxyServer server;

    private final Logger logger;
    private final Path dataDirectory;

    @Getter
    private IoriProxy core;

    private VelocityPluginMessageListener pluginMessaging;

    @Inject
    public IoriProxyVelocity(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        VelocityPlatform platform = new VelocityPlatform(this, server, logger, dataDirectory);
        core = new IoriProxy(platform);
        core.enable();
        server.getEventManager().register(this, new VelocityPresenceListener(core, server));
        server.getEventManager().register(this, new VelocityCommandBlockerListener(core));
        if (core.staffReg() != null) {
            server.getEventManager().register(this, new VelocityStaffRegListener(core, server));
        }
        server.getEventManager().register(this, new VelocityStaffChatListener(core));
        server.getEventManager().register(this, new VelocityTabCompleteListener(core));
        server.getEventManager().register(this, new VelocityDonationChatListener(core));

        pluginMessaging = new VelocityPluginMessageListener(core, server);
        server.getEventManager().register(this, pluginMessaging);
        pluginMessaging.enable();

        if (core.configs().settings().moduleEnabled("server_health")) {
            us.iorilabs.proxy.server.ServerHealthMonitor health = new us.iorilabs.proxy.server.ServerHealthMonitor(
                platform,
                core.language(),
                core.logger(),
                core.configs().commands().permission("server_health"),
                (name, onOk, onFail) ->
                    server.getServer(name).ifPresentOrElse(
                        registered ->
                            registered
                                .ping()
                                .thenAccept(ping -> onOk.run())
                                .exceptionally(ex -> {
                                    onFail.run();
                                    return null;
                                }),
                        onFail
                    )
            );
            health.start();
        }
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (pluginMessaging != null) {
            pluginMessaging.disable();
            pluginMessaging = null;
        }

        if (core != null) {
            core.disable();
            core = null;
        }
    }
}
