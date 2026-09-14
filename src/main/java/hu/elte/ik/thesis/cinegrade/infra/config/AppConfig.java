package hu.elte.ik.thesis.cinegrade.infra.config;

import hu.elte.ik.thesis.cinegrade.infra.database.AppSchemaInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public enum AppConfig {
    INSTANCE;

    private final String appName;
    private final String appVersion;
    private final String appDirectory;
    private final String logDirectory;
    private final String presetDirectory;

    private AppConfig() {
        Properties properties = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("app.properties")) {
            if (in != null) {
                properties.load(in);
            }
        } catch (IOException e) {
            Logger logger = LogManager.getLogger(AppConfig.class);
            logger.debug("Could not load app.properties: " + e.getMessage());
        }

        this.appName = properties.getProperty("app.name", "cinegrade");
        this.appVersion = properties.getProperty("app.version", "1.0-SNAPSHOT");
        this.appDirectory = Paths.get(System.getProperty("user.home"), "." + this.appName).toString();
        this.logDirectory = Paths.get(this.appDirectory, "logs").toString();
        this.presetDirectory = Paths.get(this.appDirectory, "presets").toString();
    }

    public String getAppName() {
        return appName;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public String getRootDirectory() {
        return appDirectory;
    }

    public String getLogDirectory() {
        return logDirectory;
    }

    public String getPresetDirectory() {
        return presetDirectory;
    }
}