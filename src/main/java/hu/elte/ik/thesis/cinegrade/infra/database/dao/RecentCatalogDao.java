// [AI-GENERATED]
package hu.elte.ik.thesis.cinegrade.infra.database.dao;

import hu.elte.ik.thesis.cinegrade.domain.catalog.Catalog;

import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.Date;

public class RecentCatalogDao {

    private final Connection connection;
    private static final Calendar UTC_CALENDAR = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

    public RecentCatalogDao(Connection connection) {
        this.connection = connection;
    }

    public void updateLastOpenedAt(int id, Instant now) throws SQLException {
        String sql = "UPDATE recent_catalogs SET last_opened_at = ? WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setTimestamp(1, Timestamp.from(now));
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        }
    }

    public void insert(Catalog catalog) throws SQLException {
        String sql = "INSERT INTO recent_catalogs (catalog_path, catalog_name) VALUES (?, ?) " +
                "RETURNING id, catalog_path, catalog_name, last_opened_at, created_at";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, catalog.getCatalogPath());
            pstmt.setString(2, catalog.getCatalogName());
            pstmt.executeUpdate();
        }
    }

    public Optional<Catalog> insert(String name, String path) throws SQLException {
        String sql = "INSERT INTO recent_catalogs (catalog_path, catalog_name) VALUES (?, ?) " +
                "RETURNING id, catalog_path, catalog_name, last_opened_at, created_at";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, path);
            pstmt.setString(2, name);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()){
                    return Optional.of(mapRowToCatalog(rs));
                }
            }
        }

        return Optional.empty();
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

        Timestamp lastOpenedTs = rs.getTimestamp("last_opened_at", UTC_CALENDAR);
        Timestamp createdTs = rs.getTimestamp("created_at", UTC_CALENDAR);

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

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM recent_catalogs WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }
}

