package hu.elte.ik.thesis.cinegrade.infra.process;

import hu.elte.ik.thesis.cinegrade.domain.exceptions.CineGradeException;
import hu.elte.ik.thesis.cinegrade.infra.config.AppConfig;
import hu.elte.ik.thesis.cinegrade.infra.config.SystemInfo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Optional;

public class NativeBinaryLocator {

    public static Path getBinaryPath(String binaryName) {
        String targetName = normalizeBinaryName(binaryName);

        Optional<Path> systemPath = findOsSystemPath(targetName);
        if (systemPath.isPresent()) {
            return systemPath.get();
        }

        // Check production app directory (set when packaged with jpackage)
        String appDir = System.getProperty("app.dir");
        if (appDir != null && !appDir.isBlank()) {
            Path prodBinDir = Path.of(appDir, "bin");
            Optional<Path> found = searchDirectory(prodBinDir, targetName);
            if (found.isPresent()) {
                return found.get();
            }
        }

        // Check local project directory (development in IDE)
        Path localBinDir = Path.of("bin");
        Optional<Path> foundLocal = searchDirectory(localBinDir, targetName);
        if (foundLocal.isPresent()) {
            return foundLocal.get();
        }

        // Check user home data directory (~/.cinegrade/bin)
        Path userHomeBin = Path.of(AppConfig.INSTANCE.getRootDirectory(), "bin", "windows");
        Optional<Path> foundUserHome = searchDirectory(userHomeBin, targetName);
        if (foundUserHome.isPresent()) {
            return foundUserHome.get();
        }

        throw createNotFoundException(binaryName);
    }

    public static Path getExecutablePath(String executable) {
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

        for (String directory : pathEnv.split(java.util.regex.Pattern.quote(File.pathSeparator))) {
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

    private static CineGradeException createNotFoundException(String binaryName) {
        String lower = binaryName.toLowerCase(Locale.ROOT);
        if (lower.contains("ffprobe")) {
            return new CineGradeException(hu.elte.ik.thesis.cinegrade.domain.enums.ErrorCode.REQ_FFPROBE_NOT_FOUND);
        } else if (lower.contains("ffmpeg")) {
            return new CineGradeException(hu.elte.ik.thesis.cinegrade.domain.enums.ErrorCode.REQ_FFMPEG_NOT_FOUND);
        } else if (lower.contains("exiftool")) {
            return new CineGradeException(hu.elte.ik.thesis.cinegrade.domain.enums.ErrorCode.REQ_EXIFTOOL_NOT_FOUND);
        } else if (lower.contains("libraw")) {
            return new CineGradeException(hu.elte.ik.thesis.cinegrade.domain.enums.ErrorCode.REQ_LIBRAW_DLL_MISSING, binaryName);
        }
        return new CineGradeException(hu.elte.ik.thesis.cinegrade.domain.enums.ErrorCode.REQ_RESOURCE_MISSING, binaryName);
    }
}
