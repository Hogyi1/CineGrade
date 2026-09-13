package hu.elte.ik.thesis.cinegrade.infra.database;

import hu.elte.ik.thesis.cinegrade.domain.enums.ErrorCode;
import hu.elte.ik.thesis.cinegrade.domain.exceptions.CineGradeException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class CatalogDatabase implements Database {

    private static final CatalogDatabase INSTANCE = new CatalogDatabase();
    private Connection connection;
    private Path catalogPath;
    private static final Logger logger = LogManager.getLogger(CatalogDatabase.class);


    private CatalogDatabase() {
    }

    public static CatalogDatabase getInstance() {
        return INSTANCE;
    }

    @Override
    public synchronized void openConnection(Path path) {
        if (path == null) {
            throw new CineGradeException(ErrorCode.DB_CONNECTION_FAILED, "Catalog path cannot be null");
        }

        closeConnection();
        catalogPath = path;

        try {
            logger.info("Opening catalog database: {}", catalogPath);
            this.connection = DriverManager.getConnection("jdbc:sqlite:" + catalogPath.toAbsolutePath());

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA journal_mode = WAL;");
                stmt.execute("PRAGMA foreign_keys = ON;");
            }

            SchemaInitializer.initialize(connection);
            logger.info("Catalog database opened and initialized successfully: {}", catalogPath);

        } catch (SQLException ex) {
            logger.error("Failed to open catalog database: {}", catalogPath, ex);
            closeConnection();
            throw new CineGradeException(ErrorCode.DB_CONNECTION_FAILED, catalogPath);
        }
    }

    @Override
    public synchronized void closeConnection() {
        if (!isOpen()) {
            connection = null;
            catalogPath = null;
            return;
        }

        try {
            logger.info("Closing catalog database: {}", catalogPath);
            connection.close();
            logger.info("Catalog database closed successfully");
        } catch (SQLException ex) {
            logger.error("Failed to close catalog database cleanly: {}", catalogPath, ex);
            throw new CineGradeException(ErrorCode.DB_CLOSE_FAILED, catalogPath);
        } finally {
            connection = null;
            catalogPath = null;
        }
    }

    @Override
    public Connection getConnection() {
        if (!isOpen()) {
            throw new CineGradeException(ErrorCode.DB_CONNECTION_FAILED, catalogPath != null ? catalogPath : "No catalog open");
        }
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