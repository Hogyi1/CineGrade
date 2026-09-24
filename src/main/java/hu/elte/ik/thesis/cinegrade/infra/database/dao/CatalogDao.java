package hu.elte.ik.thesis.cinegrade.infra.database.dao;

import hu.elte.ik.thesis.cinegrade.domain.catalog.Catalog;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;

public class CatalogDao {


    private final Connection connection;

    public CatalogDao(Connection connection) {
        this.connection = connection;
    }

    public void updateLastOpenedAt(int id, Date newDate) throws SQLException {
        String sql = "UPDATE recent_catalogs SET last_opened_at = ? WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.setTimestamp(2, Timestamp.from(newDate.toInstant()));
            pstmt.executeUpdate();
        }
    }

    public void insert(Catalog catalog) throws SQLException {
        String sql = "INSERT INTO recent_catalogs (catalog_path, catalog_name) VALUES (?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, catalog.getCatalogPath());
            pstmt.setString(2, catalog.getCatalogName());
            pstmt.executeUpdate();
        }
    }

    public List<Catalog> findAll() throws SQLException {
        List<Catalog> catalogs = new ArrayList<>();
        String sql = "SELECT id, catalog_path, catalog_name, last_opened_at, created_at FROM recent_catalogs ORDER BY last_opened_at";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                catalogs.add(mapRowToCatalog(rs));
            }
        }
        return catalogs;
    }

    private Catalog mapRowToCatalog(ResultSet rs) throws SQLException {
        Timestamp lastOpenedTs = rs.getTimestamp("last_opened_at");
        Timestamp createdTs = rs.getTimestamp("created_at");

        Instant lastOpenedAt = (lastOpenedTs != null) ? lastOpenedTs.toInstant() : null;
        Instant createdAt = (createdTs != null) ? createdTs.toInstant() : null;

        return new Catalog(
                rs.getInt("id"),
                rs.getString("catalog_name"),
                rs.getString("catalog_path"),
                lastOpenedAt,
                createdAt
        );
    }
}

