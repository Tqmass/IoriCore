package us.iorilabs.proxy.storage;

import static us.iorilabs.proxy.storage.MigrationStep.column;
import static us.iorilabs.proxy.storage.MigrationStep.index;
import static us.iorilabs.proxy.storage.MigrationStep.sql;
import static us.iorilabs.proxy.storage.MigrationStep.widenVarchar;

import java.util.List;

public final class SchemaCatalog {

    public static final String SHARED = "shared";
    public static final String PROXY = "proxy";
    public static final String CORE = "core";

    private SchemaCatalog() {}

    public static List<Migration> shared() {
        return List.of(
            Migration.of(
                SHARED,
                1,
                "ic_punishments",
                sql(
                    """
                    CREATE TABLE IF NOT EXISTS ic_punishments (
                      id BIGINT AUTO_INCREMENT PRIMARY KEY,
                      type VARCHAR(32) NOT NULL,
                      status VARCHAR(16) NOT NULL,
                      target_uuid VARCHAR(36) NOT NULL,
                      target_name VARCHAR(16),
                      actor_uuid VARCHAR(36),
                      actor_name VARCHAR(16),
                      reason VARCHAR(512),
                      created_at BIGINT NOT NULL,
                      expires_at BIGINT,
                      silent TINYINT(1) NOT NULL DEFAULT 0
                    )
                    """
                ),
                index("ic_punishments", "idx_ic_punishments_target", "target_uuid, status, type")
            )
        );
    }

    public static List<Migration> proxy() {
        return List.of(
            Migration.of(
                PROXY,
                1,
                "players, playtime, reports, helpop, analytics, friends, alts",
                sql(
                    """
                    CREATE TABLE IF NOT EXISTS ip_players (
                      uuid CHAR(36) NOT NULL PRIMARY KEY,
                      name VARCHAR(16) NOT NULL,
                      first_join BIGINT NOT NULL,
                      last_join BIGINT NOT NULL
                    )
                    """
                ),
                index("ip_players", "idx_ip_players_name", "name"),
                sql(
                    """
                    CREATE TABLE IF NOT EXISTS ip_playtime (
                      uuid CHAR(36) NOT NULL,
                      scope VARCHAR(64) NOT NULL,
                      millis BIGINT NOT NULL DEFAULT 0,
                      PRIMARY KEY (uuid, scope)
                    )
                    """
                ),
                sql(
                    """
                    CREATE TABLE IF NOT EXISTS ip_reports (
                      id VARCHAR(12) NOT NULL PRIMARY KEY,
                      reporter_uuid CHAR(36) NOT NULL,
                      reporter_name VARCHAR(16) NOT NULL,
                      target_name VARCHAR(16) NOT NULL,
                      reason VARCHAR(32) NOT NULL,
                      detail TEXT NOT NULL,
                      server VARCHAR(64) NOT NULL,
                      created_at BIGINT NOT NULL,
                      status VARCHAR(16) NOT NULL,
                      resolver_name VARCHAR(16) NULL
                    )
                    """
                ),
                index("ip_reports", "idx_ip_reports_status", "status"),
                sql(
                    """
                    CREATE TABLE IF NOT EXISTS ip_helpop (
                      id VARCHAR(12) NOT NULL PRIMARY KEY,
                      player_uuid CHAR(36) NOT NULL,
                      player_name VARCHAR(16) NOT NULL,
                      message TEXT NOT NULL,
                      server VARCHAR(64) NOT NULL,
                      created_at BIGINT NOT NULL,
                      open_flag TINYINT(1) NOT NULL DEFAULT 1
                    )
                    """
                ),
                index("ip_helpop", "idx_ip_helpop_open", "open_flag"),
                // ~ stat_day: "day" is reserved in H2
                sql(
                    """
                    CREATE TABLE IF NOT EXISTS ip_analytics (
                      stat_day DATE NOT NULL,
                      proxy_id VARCHAR(64) NOT NULL,
                      joins BIGINT NOT NULL DEFAULT 0,
                      peak_online INT NOT NULL DEFAULT 0,
                      PRIMARY KEY (stat_day, proxy_id)
                    )
                    """
                ),
                sql(
                    """
                    CREATE TABLE IF NOT EXISTS ip_friends (
                      uuid_a CHAR(36) NOT NULL,
                      uuid_b CHAR(36) NOT NULL,
                      since BIGINT NOT NULL,
                      PRIMARY KEY (uuid_a, uuid_b)
                    )
                    """
                ),
                index("ip_friends", "idx_ip_friends_a", "uuid_a"),
                index("ip_friends", "idx_ip_friends_b", "uuid_b"),
                sql(
                    """
                    CREATE TABLE IF NOT EXISTS ip_friend_requests (
                      from_uuid CHAR(36) NOT NULL,
                      to_uuid CHAR(36) NOT NULL,
                      created_at BIGINT NOT NULL,
                      PRIMARY KEY (from_uuid, to_uuid)
                    )
                    """
                ),
                index("ip_friend_requests", "idx_ip_friend_requests_to", "to_uuid"),
                sql(
                    """
                    CREATE TABLE IF NOT EXISTS ip_account_ips (
                      uuid CHAR(36) NOT NULL,
                      ip VARCHAR(45) NOT NULL,
                      last_seen BIGINT NOT NULL,
                      PRIMARY KEY (uuid, ip)
                    )
                    """
                ),
                index("ip_account_ips", "idx_ip_account_ips_ip", "ip")
            ),
            Migration.of(
                PROXY,
                2,
                "blacklist, staff sessions",
                sql(
                    """
                    CREATE TABLE IF NOT EXISTS ic_blacklist (
                      entry VARCHAR(64) NOT NULL PRIMARY KEY,
                      reason VARCHAR(256),
                      actor VARCHAR(64),
                      created_at BIGINT NOT NULL
                    )
                    """
                ),
                sql(
                    """
                    CREATE TABLE IF NOT EXISTS ic_staff_sessions (
                      uuid CHAR(36) NOT NULL PRIMARY KEY,
                      last_ip VARCHAR(45) NOT NULL,
                      last_country VARCHAR(8),
                      updated_at BIGINT NOT NULL
                    )
                    """
                )
            ),
            Migration.of(
                PROXY,
                3,
                "discord links, discord sync codes",
                sql(
                    """
                    CREATE TABLE IF NOT EXISTS ic_discord_links (
                      uuid CHAR(36) NOT NULL PRIMARY KEY,
                      discord_id VARCHAR(32) NOT NULL,
                      linked_at BIGINT NOT NULL
                    )
                    """
                ),
                index("ic_discord_links", "idx_ic_discord_links_discord", "discord_id"),
                sql(
                    """
                    CREATE TABLE IF NOT EXISTS ic_discord_sync_codes (
                      code VARCHAR(32) NOT NULL PRIMARY KEY,
                      uuid CHAR(36) NOT NULL,
                      name VARCHAR(16) NOT NULL,
                      expires_at BIGINT NOT NULL
                    )
                    """
                ),
                index("ic_discord_sync_codes", "idx_ic_discord_sync_codes_uuid", "uuid")
            ),
            Migration.of(PROXY, 4, "discord sync codes wider OTC", widenVarchar("ic_discord_sync_codes", "code", 32)),
            Migration.of(
                PROXY,
                5,
                "staffreg accounts devices sessions challenges audit",
                sql(
                    """
                    CREATE TABLE IF NOT EXISTS ip_staff_accounts (
                      uuid CHAR(36) NOT NULL PRIMARY KEY,
                      username VARCHAR(16) NOT NULL,
                      password_hash VARCHAR(255) NOT NULL,
                      must_change_password TINYINT(1) NOT NULL DEFAULT 1,
                      discord_id VARCHAR(32),
                      link_status VARCHAR(32) NOT NULL DEFAULT 'UNLINKED',
                      account_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
                      preferred_auth VARCHAR(16) NOT NULL DEFAULT 'DISCORD',
                      created_at BIGINT NOT NULL,
                      updated_at BIGINT NOT NULL,
                      created_by CHAR(36),
                      trust_ip_hmac VARCHAR(128),
                      trust_country VARCHAR(64),
                      trust_region VARCHAR(128),
                      trust_city VARCHAR(128),
                      trust_at BIGINT,
                      UNIQUE KEY uq_ip_staff_accounts_user (username),
                      UNIQUE KEY uq_ip_staff_accounts_discord (discord_id)
                    )
                    """
                ),
                sql(
                    """
                    CREATE TABLE IF NOT EXISTS ip_staff_devices (
                      id CHAR(36) NOT NULL PRIMARY KEY,
                      account_uuid CHAR(36) NOT NULL,
                      ip_hmac VARCHAR(128) NOT NULL,
                      country VARCHAR(64),
                      region VARCHAR(128),
                      city VARCHAR(128),
                      first_seen BIGINT NOT NULL,
                      last_seen BIGINT NOT NULL,
                      INDEX idx_ip_staff_devices_account (account_uuid)
                    )
                    """
                ),
                sql(
                    """
                    CREATE TABLE IF NOT EXISTS ip_staff_sessions (
                      id CHAR(36) NOT NULL PRIMARY KEY,
                      account_uuid CHAR(36) NOT NULL,
                      created_at BIGINT NOT NULL,
                      expires_at BIGINT,
                      revoked_at BIGINT,
                      ip_hmac VARCHAR(128),
                      server_name VARCHAR(64),
                      INDEX idx_ip_staff_sessions_account (account_uuid)
                    )
                    """
                ),
                sql(
                    """
                    CREATE TABLE IF NOT EXISTS ip_staff_challenges (
                      id CHAR(36) NOT NULL PRIMARY KEY,
                      account_uuid CHAR(36) NOT NULL,
                      kind VARCHAR(32) NOT NULL,
                      status VARCHAR(32) NOT NULL,
                      token_hash VARCHAR(128) NOT NULL,
                      discord_message_id VARCHAR(64),
                      channel_id VARCHAR(64),
                      resolved_by CHAR(36),
                      created_at BIGINT NOT NULL,
                      expires_at BIGINT NOT NULL,
                      resolved_at BIGINT,
                      INDEX idx_ip_staff_challenges_account (account_uuid, status)
                    )
                    """
                ),
                sql(
                    """
                    CREATE TABLE IF NOT EXISTS ip_staff_audit (
                      id BIGINT AUTO_INCREMENT PRIMARY KEY,
                      account_uuid CHAR(36),
                      actor_uuid CHAR(36),
                      action VARCHAR(64) NOT NULL,
                      detail VARCHAR(512),
                      created_at BIGINT NOT NULL,
                      INDEX idx_ip_staff_audit_account (account_uuid)
                    )
                    """
                )
            ),
            Migration.of(
                PROXY,
                6,
                "staffreg session/device columns",
                column("ip_staff_devices", "vpn_or_proxy", "TINYINT(1) NOT NULL DEFAULT 0"),
                column("ip_staff_sessions", "proxy_id", "VARCHAR(64)"),
                column("ip_staff_sessions", "auth_mode", "VARCHAR(16)"),
                column("ip_staff_sessions", "authenticated", "TINYINT(1) NOT NULL DEFAULT 0"),
                column("ip_staff_sessions", "step_up_granted", "TINYINT(1) NOT NULL DEFAULT 0"),
                column("ip_staff_sessions", "step_up_expires_at", "BIGINT"),
                column("ip_staff_accounts", "trust_vpn", "TINYINT(1) NOT NULL DEFAULT 0"),
                column("ip_staff_accounts", "trust_expires_at", "BIGINT")
            )
        );
    }

    public static List<Migration> core() {
        return List.of(
            Migration.of(
                CORE,
                1,
                "ranks, rank permissions, balances",
                sql(
                    """
                    CREATE TABLE IF NOT EXISTS ic_ranks (
                      id VARCHAR(36) NOT NULL PRIMARY KEY,
                      name VARCHAR(64) NOT NULL,
                      display_name VARCHAR(128),
                      prefix VARCHAR(128),
                      suffix VARCHAR(128),
                      priority INT NOT NULL DEFAULT 0,
                      type VARCHAR(32) NOT NULL,
                      color_key VARCHAR(64),
                      italic TINYINT(1) NOT NULL DEFAULT 0,
                      bold TINYINT(1) NOT NULL DEFAULT 0,
                      staff TINYINT(1) NOT NULL DEFAULT 0,
                      media TINYINT(1) NOT NULL DEFAULT 0,
                      admin TINYINT(1) NOT NULL DEFAULT 0,
                      operator TINYINT(1) NOT NULL DEFAULT 0,
                      visible TINYINT(1) NOT NULL DEFAULT 1,
                      default_rank TINYINT(1) NOT NULL DEFAULT 0,
                      scope VARCHAR(16) NOT NULL,
                      server_key VARCHAR(64) NOT NULL DEFAULT '',
                      parents TEXT
                    )
                    """
                ),
                // ~ tablas pre-runner sin estas columnas
                column("ic_ranks", "admin", "TINYINT(1) NOT NULL DEFAULT 0"),
                column("ic_ranks", "operator", "TINYINT(1) NOT NULL DEFAULT 0"),
                column("ic_ranks", "visible", "TINYINT(1) NOT NULL DEFAULT 1"),
                index("ic_ranks", "idx_ic_ranks_name_scope", "name, scope, server_key"),
                sql(
                    """
                    CREATE TABLE IF NOT EXISTS ic_rank_permissions (
                      rank_id VARCHAR(36) NOT NULL,
                      permission VARCHAR(256) NOT NULL,
                      PRIMARY KEY (rank_id, permission)
                    )
                    """
                ),
                sql(
                    """
                    CREATE TABLE IF NOT EXISTS ic_balances (
                      player_uuid VARCHAR(36) NOT NULL PRIMARY KEY,
                      balance DOUBLE NOT NULL DEFAULT 0
                    )
                    """
                )
            ),
            Migration.of(
                CORE,
                2,
                "rank keys + flags, grants",
                column("ic_ranks", "rank_key", "VARCHAR(64)"),
                column("ic_ranks", "flags", "VARCHAR(128) NOT NULL DEFAULT ''"),
                sql(
                    """
                    CREATE TABLE IF NOT EXISTS ic_grants (
                      id BIGINT AUTO_INCREMENT PRIMARY KEY,
                      player_uuid VARCHAR(36) NOT NULL,
                      rank_id VARCHAR(36) NOT NULL,
                      scope VARCHAR(16) NOT NULL,
                      server_key VARCHAR(64) NOT NULL DEFAULT '',
                      added_by VARCHAR(64),
                      reason VARCHAR(256),
                      created_at BIGINT NOT NULL,
                      expires_at BIGINT,
                      removed_at BIGINT,
                      removed_by VARCHAR(64),
                      removed_reason VARCHAR(256)
                    )
                    """
                ),
                index("ic_grants", "idx_ic_grants_player", "player_uuid, removed_at"),
                index("ic_grants", "idx_ic_grants_rank", "rank_id")
            ),
            Migration.of(
                CORE,
                3,
                "discord links, discord sync codes",
                sql(
                    """
                    CREATE TABLE IF NOT EXISTS ic_discord_links (
                      uuid CHAR(36) NOT NULL PRIMARY KEY,
                      discord_id VARCHAR(32) NOT NULL,
                      linked_at BIGINT NOT NULL
                    )
                    """
                ),
                index("ic_discord_links", "idx_ic_discord_links_discord", "discord_id"),
                sql(
                    """
                    CREATE TABLE IF NOT EXISTS ic_discord_sync_codes (
                      code VARCHAR(32) NOT NULL PRIMARY KEY,
                      uuid CHAR(36) NOT NULL,
                      name VARCHAR(16) NOT NULL,
                      expires_at BIGINT NOT NULL
                    )
                    """
                ),
                index("ic_discord_sync_codes", "idx_ic_discord_sync_codes_uuid", "uuid")
            ),
            Migration.of(CORE, 4, "discord sync codes wider OTC", widenVarchar("ic_discord_sync_codes", "code", 32))
        );
    }
}
