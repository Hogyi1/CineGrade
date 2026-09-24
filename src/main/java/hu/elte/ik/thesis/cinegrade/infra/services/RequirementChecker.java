package hu.elte.ik.thesis.cinegrade.infra.services;

import hu.elte.ik.thesis.cinegrade.domain.enums.ErrorCode;
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

import java.util.Optional;

public class RequirementChecker {

    private static final Logger logger = LogManager.getLogger(RequirementChecker.class);
    private final StringProperty currentCheck = new SimpleStringProperty();

    public void checkCriticalResources() {
        currentCheck.set("Verifying core UI resources...");

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
        currentCheck.set("Checking native binaries...");

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
        currentCheck.set("Checking FFmpeg...");

        FFmpegService service = new FFmpegService();
        logger.debug("Checking ffmpeg version...");
        Optional<String> result = service.checkFFmpegVersion(null);
        if (result.isEmpty()) {
            throw new CineGradeException(ErrorCode.REQ_FFMPEG_NOT_FOUND);
        } else {
            SystemInfo.INSTANCE.setFfmpegVersion(result.get());
            logger.debug("FFmpeg version set: {}", result.get());
        }

        currentCheck.set("Checking FFprobe...");
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
        currentCheck.set("Checking ExifTool...");

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
        currentCheck.set("Checking Java version...");
        return Runtime.version().feature() >= 21;
    }

    public ReadOnlyStringProperty currentCheckProperty() {
        return currentCheck;
    }
}
