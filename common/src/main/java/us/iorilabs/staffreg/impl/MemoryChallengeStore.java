package us.iorilabs.staffreg.impl;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import us.iorilabs.staffreg.Challenge;
import us.iorilabs.staffreg.ChallengeStore;
import us.iorilabs.staffreg.StaffRegEnums.ChallengeStatus;

public final class MemoryChallengeStore implements ChallengeStore {

    private final Map<UUID, Challenge> byId = new ConcurrentHashMap<>();

    @Override
    public CompletableFuture<Optional<Challenge>> findById(UUID id) {
        return CompletableFuture.completedFuture(Optional.ofNullable(id == null ? null : byId.get(id)));
    }

    @Override
    public CompletableFuture<Optional<Challenge>> findPendingForAccount(UUID accountUuid) {
        if (accountUuid == null) return CompletableFuture.completedFuture(Optional.empty());
        Instant now = Instant.now();
        for (Challenge challenge : byId.values()) {
            if (!accountUuid.equals(challenge.accountUuid())) continue;
            if (challenge.status() != ChallengeStatus.PENDING) continue;
            if (challenge.expiresAt() != null && challenge.expiresAt().isBefore(now)) {
                challenge.status(ChallengeStatus.EXPIRED);
                continue;
            }
            return CompletableFuture.completedFuture(Optional.of(challenge));
        }
        return CompletableFuture.completedFuture(Optional.empty());
    }

    @Override
    public CompletableFuture<Void> save(Challenge challenge) {
        if (challenge == null || challenge.id() == null) {
            return CompletableFuture.completedFuture(null);
        }
        byId.put(challenge.id(), challenge);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Boolean> consumeToken(UUID challengeId, String tokenHash) {
        if (challengeId == null || tokenHash == null || tokenHash.isBlank()) {
            return CompletableFuture.completedFuture(false);
        }
        Challenge challenge = byId.get(challengeId);
        if (challenge == null || challenge.status() != ChallengeStatus.PENDING) {
            return CompletableFuture.completedFuture(false);
        }
        Instant now = Instant.now();
        if (challenge.expiresAt() != null && challenge.expiresAt().isBefore(now)) {
            challenge.status(ChallengeStatus.EXPIRED);
            return CompletableFuture.completedFuture(false);
        }
        if (!tokenHash.equals(challenge.tokenHash())) {
            return CompletableFuture.completedFuture(false);
        }
        challenge.status(ChallengeStatus.APPROVED);
        challenge.resolvedAt(now);
        return CompletableFuture.completedFuture(true);
    }
}
