package us.iorilabs.proxy.player.transfer;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import us.iorilabs.proxy.alts.AltService;
import us.iorilabs.proxy.database.StorageService;
import us.iorilabs.proxy.logging.ProxyLogger;
import us.iorilabs.proxy.maintenance.MaintenanceService;
import us.iorilabs.proxy.platform.ProxyPlatform;
import us.iorilabs.proxy.player.PlayerDataService;
import us.iorilabs.proxy.player.ProxyPlayer;
import us.iorilabs.proxy.player.friends.FriendService;
import us.iorilabs.proxy.player.playtime.PlaytimeService;

@RequiredArgsConstructor
public final class AccountTransferService {

    private final ProxyPlatform platform;
    private final StorageService storage;
    private final ProxyLogger logger;
    private final FriendService friends;
    private final PlaytimeService playtime;
    private final PlayerDataService playerData;
    private final MaintenanceService maintenance;
    private final AltService alts;

    public Optional<String> transfer(UUID from, String fromName, UUID to, String toName) {
        if (from.equals(to)) return Optional.of("account_transfer.self");

        if (friends != null) {
            friends.transferAccount(from, to);
        }

        if (playtime != null) {
            playtime.transferAccount(from, to);
        }

        if (playerData != null) {
            playerData.transferAccount(from, to, toName);
        }

        if (alts != null) {
            alts.transferIps(from, to);
        }

        if (maintenance != null) {
            maintenance.transferWhitelist(from, fromName, to, toName);
        }

        transferPlaytimeSql(from, to);
        logger.module("Transfer", fromName + " (" + from + ") -> " + toName + " (" + to + ")");
        return Optional.empty();
    }

    public Optional<UUID> resolveUuid(String input) {
        if (input == null || input.isBlank()) return Optional.empty();

        try {
            return Optional.of(UUID.fromString(input.trim()));
        } catch (IllegalArgumentException ignored) {}

        Optional<ProxyPlayer> online = platform.findPlayerByName(input);
        if (online.isPresent()) return Optional.of(online.get().uuid());

        if (friends != null) return friends.resolveUuidPublic(input);

        return Optional.empty();
    }

    public String resolveName(UUID uuid, String fallback) {
        return platform
            .findPlayerByUuid(uuid)
            .map(ProxyPlayer::name)
            .orElse(fallback == null ? uuid.toString() : fallback);
    }

    private void transferPlaytimeSql(UUID from, UUID to) {
        if (!storage.isConnected()) return;

        platform.asyncExecutor().execute(() ->
            storage.mysql().ifPresent(mysql -> {
                try {
                    mysql.call(connection -> {
                        try {
                            try (
                                PreparedStatement select = connection.prepareStatement(
                                    "SELECT scope, millis FROM ip_playtime WHERE uuid = ?"
                                )
                            ) {
                                select.setString(1, from.toString());
                                try (ResultSet rs = select.executeQuery()) {
                                    while (rs.next()) {
                                        try (
                                            PreparedStatement upsert = connection.prepareStatement(
                                                """
                                                INSERT INTO ip_playtime (uuid, scope, millis) VALUES (?, ?, ?)
                                                ON DUPLICATE KEY UPDATE millis = millis + VALUES(millis)
                                                """
                                            )
                                        ) {
                                            upsert.setString(1, to.toString());
                                            upsert.setString(2, rs.getString(1));
                                            upsert.setLong(3, rs.getLong(2));
                                            upsert.executeUpdate();
                                        }
                                    }
                                }
                            }

                            try (
                                PreparedStatement del = connection.prepareStatement(
                                    "DELETE FROM ip_playtime WHERE uuid = ?"
                                )
                            ) {
                                del.setString(1, from.toString());
                                del.executeUpdate();
                            }
                        } catch (Exception ex) {
                            throw new RuntimeException(ex);
                        }

                        return null;
                    });
                } catch (Exception ex) {
                    logger.error("Account transfer SQL failed", ex);
                }
            })
        );
    }
}
