package us.iorilabs.staffreg.storage;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import javax.sql.DataSource;

public final class StaffRegSqlRunner {

    private final DataSource dataSource;
    private final Executor executor;

    public StaffRegSqlRunner(DataSource dataSource) {
        this(dataSource, ForkJoinPool.commonPool());
    }

    public StaffRegSqlRunner(DataSource dataSource, Executor executor) {
        this.dataSource = dataSource;
        this.executor = executor == null ? ForkJoinPool.commonPool() : executor;
    }

    public <T> CompletableFuture<T> call(SqlFunction<T> function) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = dataSource.getConnection()) {
                return function.apply(connection);
            } catch (SQLException ex) {
                throw new IllegalStateException("StaffReg SQL failed", ex);
            }
        }, executor);
    }

    public CompletableFuture<Void> run(SqlConsumer consumer) {
        return call(connection -> {
            consumer.accept(connection);
            return null;
        });
    }

    @FunctionalInterface
    public interface SqlFunction<T> {
        T apply(Connection connection) throws SQLException;
    }

    @FunctionalInterface
    public interface SqlConsumer {
        void accept(Connection connection) throws SQLException;
    }
}
