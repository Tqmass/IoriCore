package us.iorilabs.proxy.redis;

@FunctionalInterface
public interface RedisEventListener {
    void onEvent(RedisEvent event);
}
