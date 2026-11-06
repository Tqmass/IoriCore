package us.iorilabs.bukkit.donations;

import org.bukkit.Bukkit;
import us.iorilabs.api.redis.NetworkEventType;
import us.iorilabs.bukkit.IoriCorePlugin;
import us.iorilabs.bukkit.redis.CoreRedisEvent;
import us.iorilabs.bukkit.redis.CoreRedisService;
import us.iorilabs.bukkit.util.ColorUtil;
import us.iorilabs.proxy.utilities.RainbowUtils;

public final class GgWaveRedisBridge {

    private GgWaveRedisBridge() {}

    public static void register(IoriCorePlugin plugin) {
        CoreRedisService redis = plugin.redis();
        if (redis == null || !redis.enabled()) return;
        if (!plugin.settings().getBoolean("GGWAVE-CHAT.ENABLED", true)) return;
        redis.subscribe(NetworkEventType.GGWAVE_CHAT, event -> onEvent(plugin, event));
    }

    private static void onEvent(IoriCorePlugin plugin, CoreRedisEvent event) {
        if (event == null || event.data() == null) return;
        String nick = event.string("player");
        if (nick == null) nick = "?";
        String message = event.string("message");
        if (message == null) message = "";
        boolean rainbow =
            event.data().has("rainbow") && !event.data().get("rainbow").isJsonNull()
                ? event.data().get("rainbow").getAsBoolean()
                : false;
        boolean rainbowOnlyMessage =
            event.data().has("rainbowOnlyMessage") && !event.data().get("rainbowOnlyMessage").isJsonNull()
                ? event.data().get("rainbowOnlyMessage").getAsBoolean()
                : true;
        String body = message;
        if (rainbow) {
            body = RainbowUtils.colorize(message);
        }
        if (rainbow && !rainbowOnlyMessage) {
            nick = RainbowUtils.colorize(nick);
        }
        String fmt = plugin.settings().getString("GGWAVE-CHAT.FORMAT", "&7<{player}&7> &r{message}");
        String raw = fmt.replace("{player}", nick).replace("{message}", body).replace("{MESSAGE}", body);
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.broadcastMessage(ColorUtil.translate(raw)));
    }
}
