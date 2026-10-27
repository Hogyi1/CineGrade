// [AI-GENERATED]
package hu.elte.ik.thesis.cinegrade.infra.database;

import hu.elte.ik.thesis.cinegrade.domain.enums.ErrorCode;
import hu.elte.ik.thesis.cinegrade.domain.exceptions.CineGradeException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Serializes all access to one SQLite connection on a single dedicated thread.
 * <p>
 * Every call is queued on that thread, so two pieces of work can never interleave on the
 * connection: an import batch's transaction and a rating click from the FX thread simply run
 * one after the other. No locks are needed, and the caller is never blocked unless it chooses
 * to {@code join()} the returned future (which the FX thread must never do).
 * <p>
 * Calls made from inside a running transaction (on the db thread itself) run inline and become
 * part of the outer transaction, so DAO helpers can be composed without deadlocking.
 */
public final class TransactionRunner implements AutoCloseable {

    private static final Logger logger = LogManager.getLogger(TransactionRunner.class);

    /**
     * Database work that produces a result.
     */
    @FunctionalInterface
    public interface SqlWork<T> {
        T run(Connection connection) throws SQLException;
    }

    /**
     * Database work without a result.
     */
    @FunctionalInterface
    public interface SqlAction {
        void run(Connection connection) throws SQLException;
    }

    private final Connection connection;
    private final String name;
    private final ExecutorService dbThread;
    private volatile Thread owner;
    private boolean inTransaction; // only touched on the db thread

    /**
     * @param connection the connection this runner exclusively owns from now on
     * @param name       short name used for the thread and logs, e.g. "catalog"
     */
    public TransactionRunner(Connection connection, String name) {
        this.connection = connection;
        this.name = name;
        this.dbThread = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "cinegrade-db-" + name);
            t.setDaemon(true);
            owner = t;
            return t;
        });
    }

    /**
     * Runs {@code work} in one transaction: commit on success, rollback on any exception.
     *
     * @return a future completing with the work's result, or exceptionally with a {@link CineGradeException}
     */
    public <T> CompletableFuture<T> inTransaction(SqlWork<T> work) {
        if (isDbThread()) {
            return inline(() -> runInTransaction(work));
        }
        return CompletableFuture.supplyAsync(() -> runInTransaction(work), dbThread);
    }

    /**
     * Same as {@link #inTransaction(SqlWork)} for work without a result.
     * (Separate name on purpose: an overload would make lambdas like {@code c -> dao.update(..)} ambiguous.)
     */
    public CompletableFuture<Void> execute(SqlAction action) {
        return inTransaction(c -> {
            action.run(c);
            return null;
        });
    }

    /**
     * Runs read-only work on the db thread without opening a transaction.
     */
    public <T> CompletableFuture<T> read(SqlWork<T> work) {
        if (isDbThread()) {
            return inline(() -> runPlain(work));
        }
        return CompletableFuture.supplyAsync(() -> runPlain(work), dbThread);
    }

    private <T> T runInTransaction(SqlWork<T> work) {
        if (inTransaction) {
            // nested call: join the outer transaction, the outer call commits or rolls back
            return runPlain(work);
        }

        inTransaction = true;
        try {
            connection.setAutoCommit(false);
            T result = work.run(connection);
            connection.commit();
            return result;
        } catch (Exception ex) {
            rollbackQuietly();
            throw wrap(ErrorCode.DB_TRANSACTION_FAILED, ex);
        } finally {
            inTransaction = false;
            try {
                connection.setAutoCommit(true);
            } catch (SQLException ex) {
                logger.warn("[{}] Could not restore auto-commit", name, ex);
            }
        }
    }

    private <T> T runPlain(SqlWork<T> work) {
        try {
            return work.run(connection);
        } catch (Exception ex) {
            throw wrap(ErrorCode.DB_EXECUTION_FAILED, ex);
        }
    }

    private void rollbackQuietly() {
        try {
            connection.rollback();
        } catch (SQLException ex) {
            logger.error("[{}] Rollback failed", name, ex);
        }
    }

    private static <T> CompletableFuture<T> inline(Supplier<T> body) {
        try {
            return CompletableFuture.completedFuture(body.get());
        } catch (CompletionException ex) {
            return CompletableFuture.failedFuture(ex.getCause());
        }
    }

    private boolean isDbThread() {
        return Thread.currentThread() == owner;
    }

    private static CompletionException wrap(ErrorCode code, Exception ex) {
        if (ex instanceof CompletionException ce) {
            return ce;
        }
        CineGradeException cge = ex instanceof CineGradeException c ? c : new CineGradeException(code, ex, ex.getMessage());
        return new CompletionException(cge);
    }

    /**
     * Lets queued work finish (up to 10 s), then stops the db thread. Must be closed before the connection.
     */
    @Override
    public void close() {
        dbThread.shutdown();
        try {
            if (!dbThread.awaitTermination(10, TimeUnit.SECONDS)) {
                logger.warn("[{}] DB thread did not finish in time, forcing shutdown", name);
                dbThread.shutdownNow();
            }
        } catch (InterruptedException ex) {
            dbThread.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
