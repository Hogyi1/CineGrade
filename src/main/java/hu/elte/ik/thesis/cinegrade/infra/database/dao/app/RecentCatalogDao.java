// [AI-GENERATED]
package hu.elte.ik.thesis.cinegrade.infra.database.dao.app;

import hu.elte.ik.thesis.cinegrade.domain.catalog.Catalog;

import java.sql.*;
import java.time.Instant;
import java.util.*;

/**
 * Data access object for the {@code recent_catalogs} table of the application database.
 *
 * Tracks the catalogs the user has opened recently so they can be listed on the start screen.
 * The DAO does not own the {@link Connection}; the caller is responsible for its lifecycle.
 */
public class RecentCatalogDao {

    /**
     * Column list shared by every SELECT and by the INSERT ... RETURNING statement.
     */
    private static final String COLUMNS = "id, catalog_name, project_directory, project_file_path, last_opened_at, created_at";

    /**
     * Connection to the application database.
     */
    private final Connection connection;
    /**
     * Calendar used to read timestamp columns as UTC.
     */
    private static final Calendar UTC_CALENDAR = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

    /**
     * Creates a DAO operating on the given connection.
     *
     * @param connection open connection to the application database
     */
    public RecentCatalogDao(Connection connection) {
        this.connection = connection;
    }

    /**
     * Sets the {@code last_opened_at} timestamp of a recent catalog.
     *
     * @param id  id of the recent catalog row
     * @param now the new last-opened time
     * @throws SQLException if the update fails
     */
    public void updateLastOpenedAt(int id, Instant now) throws SQLException {
        String sql = "UPDATE recent_catalogs SET last_opened_at = ? WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setTimestamp(1, Timestamp.from(now));
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        }
    }

    /**
     * Inserts a new recent catalog and returns the stored row, including database-generated values
     * such as the id and the timestamps.
     *
     * @param name             display name of the catalog
     * @param projectDirectory directory containing the catalog
     * @param projectFilePath  path of the catalog's project file
     * @return the inserted catalog, or empty if the database returned no row
     * @throws SQLException if the insert fails
     */
    public Optional<Catalog> insert(String name, String projectDirectory, String projectFilePath) throws SQLException {
        String sql = "INSERT INTO recent_catalogs (catalog_name, project_directory, project_file_path) VALUES (?, ?, ?) " +
                "RETURNING " + COLUMNS;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, projectDirectory);
            pstmt.setString(3, projectFilePath);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToCatalog(rs));
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Finds a recent catalog by its project file path. The comparison is case-insensitive because
     * Windows paths are case-insensitive.
     *
     * @param projectFilePath path of the catalog's project file
     * @return the matching catalog, or empty if there is none
     * @throws SQLException if the query fails
     */
    public Optional<Catalog> findByProjectFilePath(String projectFilePath) throws SQLException {
        // Windows paths are case-insensitive
        String sql = "SELECT " + COLUMNS + " FROM recent_catalogs WHERE project_file_path = ? COLLATE NOCASE";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, projectFilePath);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToCatalog(rs));
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Returns every recent catalog, ordered by {@code last_opened_at} (ascending, so the most
     * recently opened catalog is last).
     *
     * @return all recent catalogs; empty if there are none
     * @throws SQLException if the query fails
     */
    public List<Catalog> findAll() throws SQLException {
        List<Catalog> catalogs = new ArrayList<>();
        String sql = "SELECT " + COLUMNS + " FROM recent_catalogs ORDER BY last_opened_at";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                catalogs.add(mapRowToCatalog(rs));
            }
        }
        return catalogs;
    }

    /**
     * Maps the current row of the result set to a {@link Catalog}. Timestamps are read as UTC and
     * may be {@code null}.
     *
     * @param rs result set positioned on a row containing all {@link #COLUMNS}
     * @return the mapped catalog
     * @throws SQLException if a column cannot be read
     */
    private Catalog mapRowToCatalog(ResultSet rs) throws SQLException {

        Timestamp lastOpenedTs = rs.getTimestamp("last_opened_at", UTC_CALENDAR);
        Timestamp createdTs = rs.getTimestamp("created_at", UTC_CALENDAR);

        Instant lastOpenedAt = (lastOpenedTs != null) ? lastOpenedTs.toInstant() : null;
        Instant createdAt = (createdTs != null) ? createdTs.toInstant() : null;

        return new Catalog(
                rs.getInt("id"),
                rs.getString("catalog_name"),
                rs.getString("project_directory"),
                rs.getString("project_file_path"),
                lastOpenedAt,
                createdAt
        );
    }

    /**
     * Removes a recent catalog entry. Only the database row is deleted, not the catalog on disk.
     *
     * @param id id of the recent catalog row
     * @return {@code true} if a row was deleted
     * @throws SQLException if the delete fails
     */
    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM recent_catalogs WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }
}
