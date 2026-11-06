package us.iorilabs.staffreg.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import us.iorilabs.staffreg.DeviceRecord;
import us.iorilabs.staffreg.DeviceRepo;

public final class MemoryDeviceRepo implements DeviceRepo {

    private final Map<UUID, List<DeviceRecord>> byAccount = new ConcurrentHashMap<>();

    @Override
    public CompletableFuture<List<DeviceRecord>> listForAccount(UUID accountUuid) {
        if (accountUuid == null) return CompletableFuture.completedFuture(List.of());
        return CompletableFuture.completedFuture(List.copyOf(byAccount.getOrDefault(accountUuid, List.of())));
    }

    @Override
    public CompletableFuture<Void> upsert(DeviceRecord record) {
        if (record == null || record.accountUuid() == null) return CompletableFuture.completedFuture(null);
        byAccount.compute(record.accountUuid(), (uuid, current) -> {
            List<DeviceRecord> list = current == null ? new ArrayList<>() : new ArrayList<>(current);
            list.removeIf(existing -> existing.ipHmac() != null && existing.ipHmac().equals(record.ipHmac()));
            list.add(record);
            return list;
        });
        return CompletableFuture.completedFuture(null);
    }
}
