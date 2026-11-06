package us.iorilabs.proxy.staffreg;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import us.iorilabs.proxy.database.StorageService;
import us.iorilabs.proxy.database.mysql.MySqlStorageService;
import us.iorilabs.proxy.logging.ProxyLogger;
import us.iorilabs.proxy.staffreg.sql.SqlAuditLog;
import us.iorilabs.proxy.staffreg.sql.SqlStaffAccountRepo;
import us.iorilabs.staffreg.AuditEntry;
import us.iorilabs.staffreg.AuditLog;
import us.iorilabs.staffreg.ChallengeStore;
import us.iorilabs.staffreg.DeviceRepo;
import us.iorilabs.staffreg.SessionStore;
import us.iorilabs.staffreg.StaffAccount;
import us.iorilabs.staffreg.StaffAccountRepo;
import us.iorilabs.staffreg.impl.MemoryAuditLog;
import us.iorilabs.staffreg.impl.MemoryChallengeStore;
import us.iorilabs.staffreg.impl.MemoryDeviceRepo;
import us.iorilabs.staffreg.impl.MemorySessionStore;
import us.iorilabs.staffreg.impl.MemoryStaffAccountRepo;
import us.iorilabs.staffreg.sql.SqlChallengeStore;
import us.iorilabs.staffreg.sql.SqlDeviceRepo;
import us.iorilabs.staffreg.sql.SqlSessionStore;
import us.iorilabs.staffreg.storage.StaffRegSqlRunner;

public final class StaffRegRepositories {

    private final StaffAccountRepo accounts;
    private final SessionStore sessions;
    private final ChallengeStore challenges;
    private final DeviceRepo devices;
    private final AuditLog audit;

    public StaffRegRepositories(StorageService storage, ProxyLogger logger) {
        SqlStaffAccountRepo sqlAccounts = new SqlStaffAccountRepo(storage, logger);
        SqlAuditLog sqlAudit = new SqlAuditLog(storage, logger);
        MemoryStaffAccountRepo memoryAccounts = new MemoryStaffAccountRepo();
        MemoryAuditLog memoryAudit = new MemoryAuditLog();

        this.accounts = new DelegatingStaffAccountRepo(sqlAccounts, memoryAccounts);
        this.audit = new DelegatingAuditLog(sqlAudit, memoryAudit);

        Optional<StaffRegSqlRunner> runner = runner(storage);
        if (runner.isPresent()) {
            this.sessions = new SqlSessionStore(runner.get());
            this.challenges = new SqlChallengeStore(runner.get());
            this.devices = new SqlDeviceRepo(runner.get());
        } else {
            this.sessions = new MemorySessionStore();
            this.challenges = new MemoryChallengeStore();
            this.devices = new MemoryDeviceRepo();
        }
    }

    private static Optional<StaffRegSqlRunner> runner(StorageService storage) {
        if (storage == null) return Optional.empty();
        Optional<MySqlStorageService> mysql = storage.mysql();
        if (mysql.isEmpty() || !mysql.get().isConnected() || mysql.get().dataSource() == null) {
            return Optional.empty();
        }
        return Optional.of(new StaffRegSqlRunner(mysql.get().dataSource()));
    }

    public StaffAccountRepo accounts() {
        return accounts;
    }

    public SessionStore sessions() {
        return sessions;
    }

    public ChallengeStore challenges() {
        return challenges;
    }

    public DeviceRepo devices() {
        return devices;
    }

    public AuditLog audit() {
        return audit;
    }

    private static final class DelegatingStaffAccountRepo implements StaffAccountRepo {

        private final SqlStaffAccountRepo sql;
        private final MemoryStaffAccountRepo memory;

        private DelegatingStaffAccountRepo(SqlStaffAccountRepo sql, MemoryStaffAccountRepo memory) {
            this.sql = sql;
            this.memory = memory;
        }

        @Override
        public CompletableFuture<Optional<StaffAccount>> findByUuid(UUID uuid) {
            if (!sql.available()) return memory.findByUuid(uuid);
            return sql
                .findByUuid(uuid)
                .thenCompose(opt -> opt.isPresent() ? CompletableFuture.completedFuture(opt) : memory.findByUuid(uuid));
        }

        @Override
        public CompletableFuture<Optional<StaffAccount>> findByUsername(String username) {
            if (!sql.available()) return memory.findByUsername(username);
            return sql
                .findByUsername(username)
                .thenCompose(opt ->
                    opt.isPresent() ? CompletableFuture.completedFuture(opt) : memory.findByUsername(username)
                );
        }

        @Override
        public CompletableFuture<Optional<StaffAccount>> findByDiscordId(String discordId) {
            if (!sql.available()) return memory.findByDiscordId(discordId);
            return sql
                .findByDiscordId(discordId)
                .thenCompose(opt ->
                    opt.isPresent() ? CompletableFuture.completedFuture(opt) : memory.findByDiscordId(discordId)
                );
        }

        @Override
        public CompletableFuture<Void> save(StaffAccount account) {
            return memory
                .save(account)
                .thenCompose(v -> sql.available() ? sql.save(account) : CompletableFuture.completedFuture(null));
        }
    }

    private static final class DelegatingAuditLog implements AuditLog {

        private final SqlAuditLog sql;
        private final MemoryAuditLog memory;

        private DelegatingAuditLog(SqlAuditLog sql, MemoryAuditLog memory) {
            this.sql = sql;
            this.memory = memory;
        }

        @Override
        public CompletableFuture<Void> append(AuditEntry entry) {
            return memory.append(entry).thenCompose(v -> sql.append(entry));
        }

        @Override
        public CompletableFuture<List<AuditEntry>> listForTarget(UUID targetUuid, int limit) {
            return sql
                .listForTarget(targetUuid, limit)
                .thenCompose(list ->
                    list.isEmpty() ? memory.listForTarget(targetUuid, limit) : CompletableFuture.completedFuture(list)
                );
        }
    }
}
