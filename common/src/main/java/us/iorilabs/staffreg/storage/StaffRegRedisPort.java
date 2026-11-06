package us.iorilabs.staffreg.storage;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface StaffRegRedisPort {
    CompletableFuture<Void> setex(String key, int ttlSeconds, String value);

    CompletableFuture<Optional<String>> get(String key);

    CompletableFuture<Optional<String>> getdel(String key);

    CompletableFuture<Void> del(String key);
}
