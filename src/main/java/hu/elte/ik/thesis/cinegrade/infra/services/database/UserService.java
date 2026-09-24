package hu.elte.ik.thesis.cinegrade.infra.services.database;

import com.sun.jna.platform.win32.Netapi32Util;
import hu.elte.ik.thesis.cinegrade.domain.catalog.Catalog;
import hu.elte.ik.thesis.cinegrade.domain.enums.ErrorCode;
import hu.elte.ik.thesis.cinegrade.domain.exceptions.CineGradeException;
import hu.elte.ik.thesis.cinegrade.domain.user.User;
import hu.elte.ik.thesis.cinegrade.infra.database.AppDatabase;
import hu.elte.ik.thesis.cinegrade.infra.database.dao.CatalogDao;
import hu.elte.ik.thesis.cinegrade.infra.database.dao.UserDao;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class UserService {

    private final UserDao dao;
    private final Connection connection;

    public UserService() {
        connection = AppDatabase.getInstance().getConnection();
        dao = new UserDao(connection);
    }

    public Optional<User> getActiveUser() {
        try {
            User user = dao.findUser();
            return Optional.of(user);
        } catch (SQLException ex) {
            return Optional.empty();
        }
    }

    public Path getUserAvatar() {
        try {
            User user = dao.findUser(0);
            String avatarUrl = user.getAvatarUrl();
            if (avatarUrl == null || avatarUrl.isEmpty()) {
                return Path.of("images", "default_avatar.png");
            }
            return Path.of(avatarUrl);
        } catch (SQLException ex) {
            throw new CineGradeException(ErrorCode.DB_RECORD_NOT_FOUND, "user_avatar");
        }
    }

    public void deleteUser() {
        try {
            dao.deleteUser();
        } catch (SQLException ex) {
            throw new CineGradeException(ErrorCode.DB_RECORD_NOT_FOUND, "user_to_delete");
        }
    }

    public void insertUser(User user) {
        try {
            dao.insert(user);
        } catch (SQLException ex) {
            throw new CineGradeException(ErrorCode.DB_TRANSACTION_FAILED, "insert_user");
        }
    }
}
