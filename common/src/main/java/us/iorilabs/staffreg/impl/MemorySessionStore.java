package us.iorilabs.staffreg.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import us.iorilabs.staffreg.SessionStore;
import us.iorilabs.staffreg.StaffSession;

public final class MemorySessionStore implements SessionStore {

    private final Map<UUID, StaffSession> byId = new ConcurrentHashMap<>();

    @Override
    public CompletableFuture<Optional<StaffSession>> find(UUID sessionId) {
        return CompletableFuture.completedFuture(Optional.ofNullable(sessionId == null ? null : byId.get(sessionId)));
    }

    @Override
    public CompletableFuture<List<StaffSession>> listForAccount(UUID accountUuid) {
        List<StaffSession> out = new ArrayList<>();
        if (accountUuid == null) return CompletableFuture.completedFuture(out);
        for (StaffSession session : byId.values()) {
            if (accountUuid.equals(session.accountUuid()) && session.authenticated()) {
                out.add(session);
            }
        }
        return CompletableFuture.completedFuture(out);
    }

    @Override
    public CompletableFuture<Void> save(StaffSession session) {
        if (session == null || session.sessionId() == null) return CompletableFuture.completedFuture(null);
        byId.put(session.sessionId(), session);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> revokeAll(UUID accountUuid) {
        if (accountUuid == null) return CompletableFuture.completedFuture(null);
        for (StaffSession session : byId.values()) {
            if (accountUuid.equals(session.accountUuid())) {
                session.authenticated(false);
                session.stepUpGranted(false);
                session.stepUpExpiresAt(null);
            }
        }
        return CompletableFuture.completedFuture(null);
    }
}
