package hu.elte.ik.thesis.cinegrade.app.managers.user;

import hu.elte.ik.thesis.cinegrade.domain.enums.ErrorCode;
import hu.elte.ik.thesis.cinegrade.domain.exceptions.CineGradeException;
import hu.elte.ik.thesis.cinegrade.domain.user.User;
import hu.elte.ik.thesis.cinegrade.infra.services.database.UserService;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.nio.file.Path;
import java.util.Optional;

public class UserManager {

    private final UserService userService;
    private final ObjectProperty<User> activeUser = new SimpleObjectProperty<>();

    public UserManager() {
        this.userService = new UserService();
        refreshUser();
    }

    public void refreshUser() {
        Optional<User> userOpt = userService.getActiveUser();
        activeUser.set(userOpt.orElse(null));
    }

    public boolean isLoggedIn() {
        return activeUser.get() != null;
    }

    public String getUsername() {
        User user = activeUser.get();
        return user != null ? user.getUsername() : "";
    }

    public Path getAvatarPath() {
        return userService.getUserAvatar();
    }

    public ReadOnlyObjectProperty<User> currentUserProperty() {
        return activeUser;
    }

    public void logout() {
        userService.deleteUser();
        activeUser.set(null);
    }

    public void changeUser(User user) {
        if (user != null) {
            activeUser.set(user);
            userService.insertUser(user);
        } else {
            throw new CineGradeException(ErrorCode.UNKNOWN_ERROR, "Cannot change to a null user.");
        }
    }
}
