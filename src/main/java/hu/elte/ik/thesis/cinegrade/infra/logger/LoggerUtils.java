package hu.elte.ik.thesis.cinegrade.infra.logger;
import hu.elte.ik.thesis.cinegrade.infra.config.SystemInfo;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

public class LoggerUtils {
    public static void init() {
        // TODO: set the log directory to user specified directory, or default to user home directory
        setupLogDirectory();
        setupBanner();
    }

    public static void setLogLevel(String level){
        Level logLevel = Level.toLevel(level, Level.INFO);
        setLogLevel(logLevel);
    }

    public static void setLogLevel(Level level) {
        Configurator.setRootLevel(level);
    }

    private static void setupLogDirectory() {
        String logDir = System.getProperty("cinegrade.log.dir");
        if (logDir == null || logDir.isEmpty()) {
            logDir = System.getProperty("user.home") + "/.cinegrade/logs";
            System.setProperty("cinegrade.log.dir", logDir);
        }
    }

    private static void setupBanner() {
        Logger logger = LogManager.getLogger(LoggerUtils.class);
        SystemInfo info = SystemInfo.INSTANCE;
        logger.info("Logger startup...\n");
        logger.info("=======================================");
        logger.info("        CineGrade Application          ");
        logger.info("=======================================");
        logger.info("OS:      {} {} ({})", info.getOsName(), info.getOsVersion(), info.getOsArch());
        logger.info("Java:    {} ({})", info.getJavaVersion(), info.getJavaVendor());
        logger.info("JavaFX:  {}", info.getJavafxVersion());
        logger.info("CPUs:    {}", info.getAvailableProcessors());
        logger.info("Memory:  {} MB max", info.getMaxMemoryMb());
        logger.info("Logs:    {}", System.getProperty("cinegrade.log.dir"));
        logger.info("=======================================");
        logger.info("Log level: {}", LogManager.getRootLogger().getLevel());
        logger.info("=======================================");
    }
}
