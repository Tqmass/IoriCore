package us.iorilabs.staffreg;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ChallengeStore {
    CompletableFuture<Optional<Challenge>> findById(UUID id);

    CompletableFuture<Optional<Challenge>> findPendingForAccount(UUID accountUuid);

    CompletableFuture<Void> save(Challenge challenge);

    CompletableFuture<Boolean> consumeToken(UUID challengeId, String tokenHash);
}
