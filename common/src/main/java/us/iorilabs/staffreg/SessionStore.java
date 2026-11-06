package us.iorilabs.staffreg;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface SessionStore {
    CompletableFuture<Optional<StaffSession>> find(UUID sessionId);

    CompletableFuture<List<StaffSession>> listForAccount(UUID accountUuid);

    CompletableFuture<Void> save(StaffSession session);

    CompletableFuture<Void> revokeAll(UUID accountUuid);
}
