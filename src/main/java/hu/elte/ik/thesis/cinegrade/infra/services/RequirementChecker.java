package hu.elte.ik.thesis.cinegrade.infra.services;

import com.sun.jna.Pointer;
import hu.elte.ik.thesis.cinegrade.domain.enums.ErrorCode;
import hu.elte.ik.thesis.cinegrade.domain.enums.PerformanceTier;
import hu.elte.ik.thesis.cinegrade.domain.exceptions.CineGradeException;
import hu.elte.ik.thesis.cinegrade.infra.config.SystemInfo;
import hu.elte.ik.thesis.cinegrade.infra.process.NativeBinaryLocator;
import hu.elte.ik.thesis.cinegrade.infra.services.exiftool.ExifToolService;
import hu.elte.ik.thesis.cinegrade.infra.services.ffmpeg.FFmpegService;
import hu.elte.ik.thesis.cinegrade.infra.services.libraw.LibRaw;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class RequirementChecker {

    private static final Logger logger = LogManager.getLogger(RequirementChecker.class);

    public void checkCriticalResources() {
        String[] critical = {
                "/fxmls/splashScreen.fxml",
                "/fxmls/errorModal.fxml",
                "/css/theme.css",
                "/css/splash.css",
                "/css/modal.css",
                "/images/icons/cine-logo.png"
        };

        for (String path : critical) {
            if (getClass().getResource(path) == null) {
                throw new CineGradeException(ErrorCode.REQ_RESOURCE_MISSING, path);
            }
        }
    }

    public void checkNativeBinaries() {
        if (NativeBinaryLocator.isAvailable("libraw.dll")) {
            try {
                String version = LibRaw.INSTANCE.libraw_version();
                SystemInfo.INSTANCE.setLibrawVersion(version);
                logger.debug("Libraw version: {}", version);
            } catch (Exception ex){
                throw new CineGradeException(ErrorCode.REQ_LIBRAW_LOAD_FAILED, ex, ex.getMessage());
            }
        } else {
            throw new CineGradeException(ErrorCode.REQ_LIBRAW_DLL_MISSING, "bin/windows/libraw/libraw.dll");
        }

    }

    public void checkFFmpeg() {
        FFmpegService service = new FFmpegService();
        logger.debug("Checking ffmpeg version...");
        Optional<String> result = service.checkFFmpegVersion(null);
        if (result.isEmpty()) {
            throw new CineGradeException(ErrorCode.REQ_FFMPEG_NOT_FOUND);
        } else {
            SystemInfo.INSTANCE.setFfmpegVersion(result.get());
            logger.debug("FFmpeg version set: {}", result.get());
        }

        logger.debug("Checking ffprobe version...");
        result = service.checkFFprobeVersion(null);
        if (result.isEmpty()) {
            throw new CineGradeException(ErrorCode.REQ_FFPROBE_NOT_FOUND);
        } else {
            SystemInfo.INSTANCE.setFfprobeVersion(result.get());
            logger.debug("FFprobe version set: {}", result.get());
        }

    }

    public void checkExiftool() {
        logger.debug("Checking exiftool version");
        ExifToolService service = new ExifToolService();
        Optional<String> result = service.checkExifToolVersion(null);
        if (result.isEmpty()) {
            throw new CineGradeException(ErrorCode.REQ_EXIFTOOL_NOT_FOUND);
        } else {
            SystemInfo.INSTANCE.setExiftoolVersion(result.get());
            logger.debug("Exiftool version set: {}", result.get());
        }
    }

    public boolean checkJavaVersion() {
        return Runtime.version().feature() >= 21;
    }

    public void checkAvailableSpace(Path directory, long minBytesRequired) {
        logger.debug("Checking available space in directory: {}", directory);
        try {
            FileStore fileStore = Files.getFileStore(directory);
            long usableSpace = fileStore.getUsableSpace();
            if (usableSpace < minBytesRequired) {
                throw new CineGradeException(ErrorCode.REQ_INSUFFICIENT_SPACE, directory.toString());
            }
        } catch (Exception ex) {
            throw new CineGradeException(ErrorCode.REQ_INSUFFICIENT_SPACE, directory.toString());
        }
    }

    public void runPerformanceBenchmark() {
        final long BASELINE_EXIFTOOL_MS = 300;

        // 1. ExifTool Process Spawn Benchmark
        long exifStart = System.currentTimeMillis();
        ExifToolService exifService = new ExifToolService();
        Optional<String> version = exifService.checkExifToolVersion(null);
        long exifElapsed = System.currentTimeMillis() - exifStart;

        if (version.isEmpty()) {
            throw new CineGradeException(ErrorCode.REQ_EXIFTOOL_NOT_FOUND);
        }
        SystemInfo.INSTANCE.setExiftoolVersion(version.get());

        // Calculate latency factor (e.g. 1.0 = baseline, 3.5 = slow laptop)
        double latencyRatio = (double) exifElapsed / (double) BASELINE_EXIFTOOL_MS;
        logger.info("ExifTool benchmark completed: {} ms (baseline: {} ms, ratio: {}x)",
                exifElapsed, BASELINE_EXIFTOOL_MS, latencyRatio);

        long rawStart = System.currentTimeMillis();
        Pointer pointer =LibRaw.INSTANCE.libraw_init(0);
        LibRaw.INSTANCE.libraw_close(pointer);
        long rawElapsed = System.currentTimeMillis() - rawStart;
        logger.info("LibRaw benchmark completed: {} ms", rawElapsed);

        SystemInfo.INSTANCE.setBenchmarkLatencyRatio(latencyRatio);
        if (latencyRatio > 2.5 || SystemInfo.INSTANCE.getAvailableProcessors() <= 4) {
            logger.warn("Low-tier machine detected. Recommending conservative batching & reduced preview sizes.");
            SystemInfo.INSTANCE.setPerformanceTier(PerformanceTier.LOW);
        } else if (latencyRatio < 1.0) {
            SystemInfo.INSTANCE.setPerformanceTier(PerformanceTier.HIGH);
        }
    }
}
