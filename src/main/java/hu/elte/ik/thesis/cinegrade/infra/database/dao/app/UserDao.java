// [AI-GENERATED]
package hu.elte.ik.thesis.cinegrade.infra.database.dao.app;

import hu.elte.ik.thesis.cinegrade.domain.user.User;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access object for the {@code user_session} table of the application database.
 *
 * Holds the locally persisted user session (profile data and auth token).
 * The DAO does not own the {@link Connection}; the caller is responsible for its lifecycle.
 */
public class UserDao {
    /**
     * Connection to the application database.
     */
    private final Connection connection;

    /**
     * Creates a DAO operating on the given connection.
     *
     * @param connection open connection to the application database
     */
    public UserDao(Connection connection) {
        this.connection = connection;
    }

    /**
     * Sets the last login time of a user.
     *
     * @param id      id of the user
     * @param newDate the new last login time
     * @throws SQLException if the update fails
     */
    public void updateLastLogin(int id, Instant newDate) throws SQLException {
        String sql = "UPDATE user_session SET last_login = ? WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setTimestamp(1, Timestamp.from(newDate));
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        }
    }

    /**
     * Sets the avatar URL of a user.
     *
     * @param id        id of the user
     * @param avatarUrl the new avatar URL
     * @throws SQLException if the update fails
     */
    public void updateAvatarUrl(int id, String avatarUrl) throws SQLException {
        String sql = "UPDATE user_session SET avatar_url = ? WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, avatarUrl);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        }
    }

    /**
     * Sets the auth token of a user.
     *
     * @param id        id of the user
     * @param authToken the new auth token
     * @throws SQLException if the update fails
     */
    public void updateAuthToken(int id, String authToken) throws SQLException {
        String sql = "UPDATE user_session SET auth_token = ? WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, authToken);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        }
    }

    /**
     * Inserts a user session row. The {@code last_login} and {@code created_at} columns are left to
     * their database defaults.
     *
     * @param user the user to store
     * @throws SQLException if the insert fails, e.g. when a user with the same id already exists
     */
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

    /**
     * Returns every stored user, ordered by id.
     *
     * @return all users; empty if there are none
     * @throws SQLException if the query fails
     */
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

    /**
     * Maps the current row of the result set to a {@link User}. The {@code last_login} and
     * {@code created_at} timestamps may be {@code null}.
     *
     * @param rs result set positioned on a user row
     * @return the mapped user
     * @throws SQLException if a column cannot be read
     */
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

    /**
     * Finds a user by id.
     *
     * @param id id of the user
     * @return the user, or {@code null} if there is no such user
     * @throws SQLException if the query fails
     */
    public User findLatestUser(int id) throws SQLException {
        String sql = "SELECT id, username, email, avatar_url, auth_token, auth_token, last_login, created_at FROM user_session WHERE id = ?";
        User user;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()){
                return mapRowToUser(rs);
            }
        }
        return null;
    }

    /**
     * Returns a single user, selected by {@code ORDER BY last_login LIMIT 1}.
     *
     * Note: the ordering is ascending, so this selects the user with the <em>oldest</em> login time.
     *
     * @return the selected user, or {@code null} if no user is stored
     * @throws SQLException if the query fails
     */
    public User findLatestUser() throws SQLException {
        String sql = "SELECT id, username, email, avatar_url, auth_token, auth_token, last_login, created_at FROM user_session ORDER BY last_login LIMIT 1";
        User user;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapRowToUser(rs);
            }
        }
        return null;
    }

    /**
     * Deletes every row of the {@code user_session} table, i.e. clears the stored session.
     *
     * @return always {@code true} if the statement ran without error
     * @throws SQLException if the statement fails
     */
    public boolean deleteUser() throws SQLException {
        String sql = "DELETE FROM user_session";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            ResultSet rs = pstmt.executeQuery();
            return true;
        }
    }
}
