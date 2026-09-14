package hu.elte.ik.thesis.cinegrade.infra.database;

import hu.elte.ik.thesis.cinegrade.domain.enums.ErrorCode;
import hu.elte.ik.thesis.cinegrade.domain.exceptions.CineGradeException;
import hu.elte.ik.thesis.cinegrade.infra.process.ProcessRunner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class AppDatabase implements Database {

    private static final AppDatabase INSTANCE = new AppDatabase();
    private Connection connection;
    private Path appDbPath;
    private static final Logger logger = LogManager.getLogger(AppDatabase.class);

    private AppDatabase() {
    }

    public static AppDatabase getInstance() {
        return INSTANCE;
    }

    @Override
    public synchronized boolean openConnection(Path path) {
        closeConnection();
        this.appDbPath = path;

        if (appDbPath == null) {
            logger.warn("No path provided for AppDatabase");
            return false;
        }

        try {
            logger.info("Opening app database: {}", appDbPath);
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + appDbPath.toAbsolutePath());

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA journal_mode = WAL;");
                stmt.execute("PRAGMA foreign_keys = ON;");
            }

            AppSchemaInitializer.initialize(connection);
            logger.info("App database opened and initialized successfully: {}", appDbPath);
            return true;
        } catch (SQLException ex) {
            closeConnection();
            throw new CineGradeException(ErrorCode.DB_CONNECTION_FAILED, ex, appDbPath);
        }
    }

    @Override
    public synchronized void closeConnection() {
        if (!isOpen()) {
            connection = null;
            appDbPath = null;
            return;
        }

        try {
            logger.info("Closing app database: {}", appDbPath);
            connection.close();
            logger.info("App database closed successfully");
        } catch (SQLException ex) {
            throw new CineGradeException(ErrorCode.DB_CLOSE_FAILED, ex, appDbPath);
        } finally {
            connection = null;
            appDbPath = null;
        }
    }

    @Override
    public Connection getConnection() {
        return connection;
    }

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
