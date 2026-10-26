package hu.elte.ik.thesis.cinegrade.infra.services.storage;

import hu.elte.ik.thesis.cinegrade.domain.enums.ErrorCode;
import hu.elte.ik.thesis.cinegrade.domain.exceptions.CineGradeException;
import hu.elte.ik.thesis.cinegrade.domain.tasks.appinit.AppInitService;
import hu.elte.ik.thesis.cinegrade.infra.config.AppConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashSet;
import java.util.UUID;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class StorageService {

    private final static int MAX_DELETE_DEPTH = 10;
    private static final Logger logger = LogManager.getLogger(StorageService.class);
    private final HashSet<Path> allowedRoots = new HashSet<>();

    public StorageService() {
        addAllowedRoot(Path.of(AppConfig.INSTANCE.getRootDirectory()));
    }

    public StorageService(Path... allowedRoots) {
        addAllowedRoot(Path.of(AppConfig.INSTANCE.getRootDirectory()));
        for (Path root : allowedRoots) {
            addAllowedRoot(root);
        }
    }

    /**
     * Adds an allowed root directory.
     *
     * @param root the root directory to allow
     */
    public void addAllowedRoot(Path root) {
        allowedRoots.add(root);
    }

    /**
     * Ensures that the specified directory exists.
     *
     * @param dir the directory to ensure
     */
    public Path ensureDirectory(Path dir) {
        if (!Files.exists(dir)) {
            try {
                return Files.createDirectories(dir);
            } catch (Exception e) {
                throw new CineGradeException(ErrorCode.FOLDER_CREATE_FAILED, e);
            }
        }
        return dir;
    }

    /**
     * Cleans the specified directory by deleting all its contents.
     * Leaves the directory itself intact.
     *
     * @param dir the directory to clean
     * @return {@code true} if the directory was successfully cleaned, {@code false} otherwise
     */
    public boolean cleanDirectory(Path dir) {
        if (!isSafeToDelete(dir)) {
            logger.warn("cleanDirectory blocked for unauthorized directory: {}", dir);
            return false;
        }

        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            return true;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path entry : stream) {
                deleteRecursively(entry, 0);
            }
        } catch (IOException e) {
            logger.warn("Failed to read temp directory during cleanup: {}", dir, e);
            return false;
        }
        return true;
    }

    /**
     * Deletes the specified directory and all its contents.
     *
     * @param dir the directory to delete
     * @return {@code true} if the directory was successfully deleted, {@code false} otherwise
     */
    public boolean deleteDirectory(Path dir) {
        if (!isSafeToDelete(dir)) {
            logger.warn("deleteDirectory blocked for unauthorized directory: {}", dir);
            return false;
        }

        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            return true;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path entry : stream) {
                deleteRecursively(entry, 0);
            }
            Files.deleteIfExists(dir);
        } catch (IOException e) {
            logger.warn("Failed to read temp directory during cleanup: {}", dir, e);
            return false;
        }
        return true;
    }

    /**
     * Checks if there is sufficient space available in the specified directory.
     *
     * @param targetDir   the directory to check
     * @param spaceNeeded the amount of space needed
     * @return {@code true} if there is sufficient space, {@code false} otherwise
     */
    public boolean sufficientSpace(Path targetDir, long spaceNeeded) {
        try {
            FileStore fileStore = Files.getFileStore(targetDir);
            long availableSpace = fileStore.getUsableSpace();
            return availableSpace >= spaceNeeded;
        } catch (IOException e) {
            logger.warn("Failed to check available space for directory: {}", targetDir, e);
            return false;
        }
    }

    /**
     * Performs an atomic move operation.
     *
     * @param source the source path
     * @param target the target path
     * @return the target path if the operation was successful, {@code null} otherwise
     */
    public Path atomicMove(Path source, Path target) {
        try {
            ensureDirectory(target.getParent());
            try {
                return Files.move(source, target, ATOMIC_MOVE, REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                return Files.move(source, target, REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            logger.warn("Failed to perform atomic move: {}", source, ex);
            return null;
        }
    }

    /**
     * Performs an atomic copy operation.
     *
     * @param source the source path
     * @param target the target path
     * @return the target path if the operation was successful, {@code null} otherwise
     */
    public Path atomicCopy(Path source, Path target) {
        try {
            ensureDirectory(target.getParent());

            Path tempTarget = target.resolveSibling(target.getFileName() + "." + UUID.randomUUID() + ".tmp");
            Files.copy(source, tempTarget, StandardCopyOption.REPLACE_EXISTING);

            try {
                Files.move(tempTarget, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tempTarget, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return target;
        } catch (IOException ex) {
            logger.error("Failed to copy file atomically from {} to {}", source, target, ex);
            throw new CineGradeException(ErrorCode.FILE_WRITE_FAILED, ex, target.toString());
        }
    }

    /**
     * Checks if it's safe to delete the specified path.
     *
     * @param path the path to check
     * @return {@code true} if it's safe to delete, {@code false} otherwise
     */
    private boolean isSafeToDelete(Path path) {
        if (path == null) {
            return false;
        }

        Path normalizedPath = path.toAbsolutePath().normalize();

        // Never allow deleting root drives or top-level system folders
        if (normalizedPath.getNameCount() <= 1 || normalizedPath.getRoot().equals(normalizedPath)) {
            logger.error("Security alert: Attempted to delete root/system directory: {}",
                    normalizedPath);
            return false;
        }

        for (Path allowedRoot : allowedRoots) {
            if (normalizedPath.startsWith(allowedRoot)) {
                return true;
            }
        }

        logger.error("Access denied: Path '{}' is outside the permitted catalog roots: {}",
                normalizedPath, allowedRoots);
        return false;
    }

    /**
     * Recursively deletes the specified path.
     *
     * @param path  the path to delete
     * @param round the current deletion round
     */
    private void deleteRecursively(Path path, int round) {
        round++;
        if (round >= MAX_DELETE_DEPTH)
            return;
        try {
            if (Files.isDirectory(path)) {
                try (DirectoryStream<Path> entries = Files.newDirectoryStream(path)) {
                    for (Path child : entries) {
                        deleteRecursively(child, round);
                    }
                }
            }
            Files.deleteIfExists(path);
        } catch (IOException e) {
            LogManager.getLogger(AppInitService.class).warn("Could not delete stale temp file (might be locked): {}", path, e);
        }
    }
}
