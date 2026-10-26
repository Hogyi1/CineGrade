package hu.elte.ik.thesis.cinegrade.infra.database;

import hu.elte.ik.thesis.cinegrade.domain.enums.ErrorCode;
import hu.elte.ik.thesis.cinegrade.domain.exceptions.CineGradeException;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.Consumer;

/**
 * One SQLite file with one connection, opened in the constructor and closed in close().
 * Subclasses only decide which schema gets initialized, so every per-catalog database
 * can be pushed onto the scope's ResourceStack the same way.
 */
public abstract class SqliteDatabase implements Database {

    private final Logger logger = LogManager.getLogger(getClass());
    private final Consumer<Connection> schemaInitializer;
    private Connection connection;
    @Getter
    private Path path;

    protected SqliteDatabase(Path path, Consumer<Connection> schemaInitializer) {
        this.schemaInitializer = schemaInitializer;
        openConnection(path);
    }

    @Override
    public synchronized boolean openConnection(Path path) {
        closeConnection();

        if (path == null) {
            throw new CineGradeException(ErrorCode.DB_CONNECTION_FAILED, "Database path cannot be null");
        }

        this.path = path;

        try {
            logger.info("Opening database: {}", path);
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + path.toAbsolutePath());

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA journal_mode = WAL;");
                stmt.execute("PRAGMA foreign_keys = ON;");
            }

            schemaInitializer.accept(connection);
            logger.info("Database opened and initialized successfully: {}", path);
            return true;
        } catch (SQLException ex) {
            closeConnection();
            throw new CineGradeException(ErrorCode.DB_CONNECTION_FAILED, ex, path);
        }
    }

    @Override
    public synchronized void closeConnection() {
        if (!isOpen()) {
            connection = null;
            return;
        }

        try {
            logger.info("Closing database: {}", path);
            connection.close();
            logger.info("Database closed successfully: {}", path);
        } catch (SQLException ex) {
            throw new CineGradeException(ErrorCode.DB_CLOSE_FAILED, ex, path);
        } finally {
            connection = null;
        }
    }

    @Override
    public Connection getConnection() {
        if (!isOpen()) {
            throw new CineGradeException(ErrorCode.DB_CONNECTION_FAILED, path != null ? path : "No database open");
        }
        return connection;
    }

    @Override
    public boolean isOpen() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException ex) {
            return false;
        }
    }

    @Override
    public void close() {
        closeConnection();
    }
}
