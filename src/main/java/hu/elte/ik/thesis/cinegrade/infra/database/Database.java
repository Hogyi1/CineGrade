package hu.elte.ik.thesis.cinegrade.infra.database;

import java.nio.file.Path;
import java.sql.Connection;

public interface Database extends AutoCloseable{
    void openConnection(Path path);
    void closeConnection();
    Connection getConnection();
}
