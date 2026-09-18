package hu.elte.ik.thesis.cinegrade.app.main;

import hu.elte.ik.thesis.cinegrade.infra.logger.LoggerUtils;
import hu.elte.ik.thesis.cinegrade.javafx.ui.dialog.ErrorHandler;
import org.apache.logging.log4j.LogManager;

public class Launcher {
    public static void main(String[] args) {
        LoggerUtils.init();
        LogManager.getLogger().info("Application started.");
        ErrorHandler.getInstance().install();
        App.launch(App.class, args);
    }
}
