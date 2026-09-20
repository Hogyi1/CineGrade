package hu.elte.ik.thesis.cinegrade.infra.process;

import hu.elte.ik.thesis.cinegrade.domain.exceptions.CineGradeException;
import hu.elte.ik.thesis.cinegrade.infra.config.AppConfig;
import hu.elte.ik.thesis.cinegrade.infra.config.SystemInfo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class NativeBinaryLocator {

    private static final ConcurrentHashMap<String, Optional<Path>> CACHE = new ConcurrentHashMap<>();

    public static Optional<Path> getBinaryPath(String binaryName) {
        String targetName = normalizeBinaryName(binaryName);
        return CACHE.computeIfAbsent(targetName, NativeBinaryLocator::resolveBinaryPath);
    }

    private static Optional<Path> resolveBinaryPath(String binaryName) {
        String targetName = normalizeBinaryName(binaryName);

        Optional<Path> systemPath = findOsSystemPath(targetName);
        if (systemPath.isPresent()) {
            return systemPath;
        }

        // Check production app directory (set when packaged with jpackage)
        String appDir = System.getProperty("app.dir");
        if (appDir != null && !appDir.isBlank()) {
            Path prodBinDir = Path.of(appDir, "bin");
            Optional<Path> found = searchDirectory(prodBinDir, targetName);
            if (found.isPresent()) {
                return found;
            }
        }

        // Check local project directory (development in IDE)
        Path localBinDir = Path.of("bin");
        Optional<Path> foundLocal = searchDirectory(localBinDir, targetName);
        if (foundLocal.isPresent()) {
            return foundLocal;
        }

        Path currentDirFile = Path.of(targetName);
        if (Files.isRegularFile(currentDirFile)) {
            Optional<Path> foundRoot = Optional.of(currentDirFile.toAbsolutePath());
            System.out.println("Working Directory: " + Path.of("").toAbsolutePath());
            System.out.println("Resolved Target: " + currentDirFile.toAbsolutePath());
            return foundRoot;
        }

        // Check user home data directory (~/.cinegrade/bin)
        Path userHomeBin = Path.of(AppConfig.INSTANCE.getRootDirectory(), "bin", "windows");
        Optional<Path> foundUserHome = searchDirectory(userHomeBin, targetName);
        if (foundUserHome.isPresent()) {
            return foundUserHome;
        }

        return Optional.empty();
    }

    public static Optional<Path> getExecutablePath(String executable) {
        return getBinaryPath(executable);
    }

    private static Optional<Path> searchDirectory(Path baseDir, String targetFileName) {
        if (baseDir == null || !Files.exists(baseDir) || !Files.isDirectory(baseDir)) {
            return Optional.empty();
        }

        try (var stream = Files.walk(baseDir)) {
            return stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().equalsIgnoreCase(targetFileName))
                    .findFirst()
                    .map(Path::toAbsolutePath);
        } catch (IOException ex) {
            return Optional.empty();
        }
    }

    private static Optional<Path> findOsSystemPath(String targetName) {
        String pathEnv = System.getenv("PATH");
        if (pathEnv == null || pathEnv.isBlank()) {
            return Optional.empty();
        }

        for (String directory : pathEnv.split(Pattern.quote(File.pathSeparator))) {
            try {
                Path filePath = Path.of(directory.trim(), targetName);
                if (Files.isRegularFile(filePath) && Files.isExecutable(filePath)) {
                    return Optional.of(filePath.toAbsolutePath());
                }
            } catch (Exception ignored) {

            }
        }

        return Optional.empty();
    }

    private static String normalizeBinaryName(String name) {
        String os = SystemInfo.INSTANCE.getOsName().toLowerCase(Locale.ROOT);
        if (os.contains("win") && !name.contains(".")) {
            return name + ".exe";
        }
        return name;
    }

    public static boolean isAvailable(String binaryName) {
        return getBinaryPath(binaryName).isPresent();
    }
}
