package hu.elte.ik.thesis.cinegrade.app.main;

import hu.elte.ik.thesis.cinegrade.infra.logger.LoggerUtils;
import org.apache.logging.log4j.LogManager;

public class Launcher {
    public static void main(String[] args) {
        LoggerUtils.init();
        App.launch(App.class, args);
        LogManager.getLogger().info("Application started.");
    }
}
