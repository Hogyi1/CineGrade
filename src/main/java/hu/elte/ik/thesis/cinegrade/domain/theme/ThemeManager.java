package hu.elte.ik.thesis.cinegrade.domain.theme;


import hu.elte.ik.thesis.cinegrade.domain.enums.ErrorCode;
import hu.elte.ik.thesis.cinegrade.domain.enums.Theme;
import hu.elte.ik.thesis.cinegrade.domain.exceptions.CineGradeException;
import hu.elte.ik.thesis.cinegrade.infra.config.AppConfig;
import hu.elte.ik.thesis.cinegrade.infra.database.dao.SettingsDao;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;

public class ThemeManager {

    private static final Logger logger = LogManager.getLogger(ThemeManager.class);
    private static final ThemeManager INSTANCE = new ThemeManager();
    private static final String SETTING_KEY = "app.theme";

    private final Theme fallBackTheme = AppConfig.INSTANCE.getFallBackTheme();
    private final ObjectProperty<Theme> activeTheme = new SimpleObjectProperty<>(fallBackTheme);
    private SettingsDao settingsDao;

    private ThemeManager() {}

    public static ThemeManager getInstance() {
        return INSTANCE;
    }

    public static void toggle() {
        getInstance().toggleTheme();
    }

    public static void set(Theme theme) {
        getInstance().setTheme(theme);
    }

    public static Theme get() {
        return getInstance().getTheme();
    }

    public void initDatabase(Connection connection) {
        this.settingsDao = new SettingsDao(connection);
        try {
            String saved = settingsDao.get(SETTING_KEY, fallBackTheme.name());
            activeTheme.set(Theme.valueOf(saved));
            logger.info("Loaded theme from database: {}", activeTheme.get());
        } catch (Exception e) {
            logger.warn("Could not load theme from database, defaulting to DARK", e);
            activeTheme.set(Theme.DARK);
        }
    }

    public void toggleTheme() {
        setTheme(getTheme() == Theme.DARK ? Theme.LIGHT : Theme.DARK);
    }

    public void setTheme(Theme newTheme) {
        if (newTheme == null || newTheme == activeTheme.get()) return;

        activeTheme.set(newTheme);
        logger.info("Theme switched to: {}", newTheme);

        if (settingsDao != null) {
            try {
                settingsDao.set(SETTING_KEY, newTheme.name());
            } catch (SQLException e) {
                throw new CineGradeException(ErrorCode.DB_TRANSACTION_FAILED, e, SETTING_KEY);
            }
        }
    }

    public ReadOnlyObjectProperty<Theme> themeProperty() {
        return activeTheme;
    }

    public Theme getTheme() {
        return activeTheme.get();
    }
}
