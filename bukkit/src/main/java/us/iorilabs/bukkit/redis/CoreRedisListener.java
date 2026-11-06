package us.iorilabs.bukkit.redis;

@FunctionalInterface
public interface CoreRedisListener {
    void onEvent(CoreRedisEvent event);
}
