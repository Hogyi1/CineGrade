package hu.elte.ik.thesis.cinegrade.infra.database.dao;

import hu.elte.ik.thesis.cinegrade.domain.catalog.Catalog;
import hu.elte.ik.thesis.cinegrade.domain.user.User;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class UserDao {
    private final Connection connection;

    public UserDao(Connection connection) {
        this.connection = connection;
    }

    public void updateLastLogin(int id, Instant newDate) throws SQLException {
        String sql = "UPDATE user_session SET last_login = ? WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setTimestamp(1, Timestamp.from(newDate));
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        }
    }

    public void updateAvatarUrl(int id, String avatarUrl) throws SQLException {
        String sql = "UPDATE user_session SET avatar_url = ? WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, avatarUrl);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        }
    }

    public void updateAuthToken(int id, String authToken) throws SQLException {
        String sql = "UPDATE user_session SET auth_token = ? WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, authToken);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        }
    }

    public void insert(User user) throws SQLException {
        String sql = "INSERT INTO user_session (id, username, email, avatar_url, auth_token) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, user.getId());
            pstmt.setString(2, user.getUsername());
            pstmt.setString(3, user.getEmail());
            pstmt.setString(4, user.getAvatarUrl());
            pstmt.setString(5, user.getAuthToken());
            pstmt.executeUpdate();
        }
    }

    public List<User> findAll() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT id, username, email, avatar_url, auth_token, auth_token, last_login, created_at FROM user_session ORDER BY id";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                users.add(mapRowToUser(rs));
            }
        }
        return users;
    }

    private User mapRowToUser(ResultSet rs) throws SQLException {
        Timestamp lastLogin = rs.getTimestamp("last_login");
        Timestamp createdTs = rs.getTimestamp("created_at");

        Instant lastLoginAt = (lastLogin != null) ? lastLogin.toInstant() : null;
        Instant createdAt = (createdTs != null) ? createdTs.toInstant() : null;

        return new User(
                rs.getInt("id"),
                rs.getString("username"),
                rs.getString("email"),
                rs.getString("avatar_url"),
                rs.getString("auth_token"),
                lastLoginAt,
                createdAt
        );
    }

    public User findUser(int id) throws SQLException {
        String sql = "SELECT id, username, email, avatar_url, auth_token, auth_token, last_login, created_at FROM user_session WHERE id = ?";
        User user;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            user = mapRowToUser(rs);
        }
        return user;
    }

    public User findUser() throws SQLException {
        String sql = "SELECT id, username, email, avatar_url, auth_token, auth_token, last_login, created_at FROM user_session ORDER BY last_login LIMIT 1";
        User user;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            ResultSet rs = pstmt.executeQuery();
            return mapRowToUser(rs);
        }
    }

    public boolean deleteUser() throws SQLException {
        String sql = "DELETE FROM user_session";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            ResultSet rs = pstmt.executeQuery();
            return true;
        }
    }
}
