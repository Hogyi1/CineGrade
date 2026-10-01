// [AI-GENERATED]
package hu.elte.ik.thesis.cinegrade.infra.database.dao;

import hu.elte.ik.thesis.cinegrade.domain.catalog.CatalogFolder;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CatalogsFolderDao {

    private final Connection connection;

    public CatalogsFolderDao(Connection connection) {
        this.connection = connection;
    }

    public CatalogFolder insert(String name, Integer parentId) throws SQLException {
        String sql = "INSERT INTO catalog_folders (name, parent_id) VALUES (?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, name);
            if (parentId != null) {
                pstmt.setInt(2, parentId);
            } else {
                pstmt.setNull(2, Types.INTEGER);
            }
            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int generatedId = rs.getInt(1);
                    return new CatalogFolder(generatedId, name, parentId);
                }
            }
        }
        return new CatalogFolder(0, name, parentId);
    }

    public void insert(CatalogFolder folder) throws SQLException {
        if (folder == null) return;
        CatalogFolder created = insert(folder.getName(), folder.getParentId());
        folder.setId(created.getId());
    }

    public Optional<CatalogFolder> findById(int id) throws SQLException {
        String sql = "SELECT id, name, parent_id FROM catalog_folders WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToFolder(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<CatalogFolder> findAll() throws SQLException {
        List<CatalogFolder> folders = new ArrayList<>();
        String sql = "SELECT id, name, parent_id FROM catalog_folders ORDER BY name";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                folders.add(mapRowToFolder(rs));
            }
        }
        return folders;
    }

    public List<CatalogFolder> findByParentId(Integer parentId) throws SQLException {
        List<CatalogFolder> folders = new ArrayList<>();
        String sql = parentId == null
                ? "SELECT id, name, parent_id FROM catalog_folders WHERE parent_id IS NULL ORDER BY name"
                : "SELECT id, name, parent_id FROM catalog_folders WHERE parent_id = ? ORDER BY name";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            if (parentId != null) {
                pstmt.setInt(1, parentId);
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    folders.add(mapRowToFolder(rs));
                }
            }
        }
        return folders;
    }

    public List<CatalogFolder> getRootFolders() throws SQLException {
        return findByParentId(null);
    }

    public boolean updateName(int id, String newName) throws SQLException {
        String sql = "UPDATE catalog_folders SET name = ? WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, newName);
            pstmt.setInt(2, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean update(CatalogFolder folder) throws SQLException {
        if (folder == null) return false;
        String sql = "UPDATE catalog_folders SET name = ?, parent_id = ? WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, folder.getName());
            if (folder.getParentId() != null) {
                pstmt.setInt(2, folder.getParentId());
            } else {
                pstmt.setNull(2, Types.INTEGER);
            }
            pstmt.setInt(3, folder.getId());
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM catalog_folders WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    private CatalogFolder mapRowToFolder(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String name = rs.getString("name");
        int pId = rs.getInt("parent_id");
        Integer parentId = rs.wasNull() ? null : pId;
        return new CatalogFolder(id, name, parentId);
    }
}
