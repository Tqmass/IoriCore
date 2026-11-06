package us.iorilabs.staffreg.impl;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import us.iorilabs.staffreg.StaffAccount;
import us.iorilabs.staffreg.StaffAccountRepo;

public final class MemoryStaffAccountRepo implements StaffAccountRepo {

    private final Map<UUID, StaffAccount> byUuid = new ConcurrentHashMap<>();
    private final Map<String, UUID> byUsername = new ConcurrentHashMap<>();
    private final Map<String, UUID> byDiscord = new ConcurrentHashMap<>();

    @Override
    public CompletableFuture<Optional<StaffAccount>> findByUuid(UUID uuid) {
        return CompletableFuture.completedFuture(Optional.ofNullable(uuid == null ? null : byUuid.get(uuid)));
    }

    @Override
    public CompletableFuture<Optional<StaffAccount>> findByUsername(String username) {
        if (username == null || username.isBlank()) return CompletableFuture.completedFuture(Optional.empty());
        UUID id = byUsername.get(username.toLowerCase(Locale.ROOT));
        return findByUuid(id);
    }

    @Override
    public CompletableFuture<Optional<StaffAccount>> findByDiscordId(String discordId) {
        if (discordId == null || discordId.isBlank()) return CompletableFuture.completedFuture(Optional.empty());
        return findByUuid(byDiscord.get(discordId));
    }

    @Override
    public CompletableFuture<Void> save(StaffAccount account) {
        if (account == null || account.uuid() == null) return CompletableFuture.completedFuture(null);
        byUuid.put(account.uuid(), account);
        if (account.username() != null && !account.username().isBlank()) {
            byUsername.put(account.username().toLowerCase(Locale.ROOT), account.uuid());
        }
        if (account.discordId() != null && !account.discordId().isBlank()) {
            byDiscord.put(account.discordId(), account.uuid());
        }
        return CompletableFuture.completedFuture(null);
    }
}
