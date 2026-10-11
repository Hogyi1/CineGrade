package hu.elte.ik.thesis.cinegrade.infra.storage.database;

import java.nio.file.Path;
import java.sql.Connection;

public interface Database extends AutoCloseable{
    boolean openConnection(Path path);
    void closeConnection();
    Connection getConnection();
}
