package us.iorilabs.staffreg;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface DeviceRepo {
    CompletableFuture<List<DeviceRecord>> listForAccount(UUID accountUuid);

    CompletableFuture<Void> upsert(DeviceRecord record);
}
