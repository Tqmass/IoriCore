package us.iorilabs.proxy.commands;

import com.google.gson.JsonObject;
import java.util.List;
import java.util.Map;
import us.iorilabs.proxy.IoriProxy;
import us.iorilabs.proxy.redis.ProxyEventType;
import us.iorilabs.proxy.redis.RedisEvent;
import us.iorilabs.proxy.redis.RedisService;

public final class RedisTestCommand implements ProxyCommand {

    private final IoriProxy proxy;

    public RedisTestCommand(IoriProxy proxy) {
        this.proxy = proxy;
    }

    @Override
    public String name() {
        return "redistest";
    }

    @Override
    public List<String> aliases() {
        return List.of();
    }

    @Override
    public String permission() {
        return proxy.configs().commands().permission("redistest");
    }

    @Override
    public void execute(CommandSource source, String label, String[] args) {
        RedisService redis = proxy.redis();
        source.sendMessage(msg("general.redis_test_start", Map.of()));
        source.sendMessage("<gray>Status: <white>" + redis.statusLine() + "</white></gray>");

        if (!redis.isConnected()) {
            source.sendMessage(msg("general.redis_not_connected", Map.of()));
            return;
        }

        JsonObject data = new JsonObject();
        data.addProperty("sender", source.name());
        data.addProperty("message", "ping");
        RedisEvent event = new RedisEvent(ProxyEventType.REDIS_TEST.id(), redis.proxyId(), data);
        redis.publish(event);

        String ping = redis.withJedis(jedis -> jedis.ping()).orElse("FAIL");
        source.sendMessage(msg("general.redis_test_ok", Map.of("ping", ping, "event_id", event.eventId())));
    }

    private String msg(String key, Map<String, String> placeholders) {
        return proxy.language().raw(key, placeholders);
    }
}
