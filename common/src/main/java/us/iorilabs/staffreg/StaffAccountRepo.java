package us.iorilabs.staffreg;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface StaffAccountRepo {
    CompletableFuture<Optional<StaffAccount>> findByUuid(UUID uuid);

    CompletableFuture<Optional<StaffAccount>> findByUsername(String username);

    CompletableFuture<Optional<StaffAccount>> findByDiscordId(String discordId);

    CompletableFuture<Void> save(StaffAccount account);
}
