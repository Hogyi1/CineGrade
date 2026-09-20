package hu.elte.ik.thesis.cinegrade.infra.services.ffmpeg;

import hu.elte.ik.thesis.cinegrade.domain.results.ProcessResult;
import hu.elte.ik.thesis.cinegrade.infra.process.ProcessRunner;
import hu.elte.ik.thesis.cinegrade.infra.services.RequirementChecker;
import hu.elte.ik.thesis.cinegrade.infra.services.exiftool.ExifToolCommandBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FFmpegService {

    private static final Logger logger = LogManager.getLogger(FFmpegService.class);
    private final Pattern VERSION_PATTERN = Pattern.compile("(?:ffmpeg|ffprobe) version\\s+([^\\s]+)", Pattern.CASE_INSENSITIVE);

    public boolean validateInput(Path path, BooleanSupplier isCanceled) {
        logger.debug("Validating input media file: {}", path);
        FFmpegCommandBuilder builder = new FFmpegCommandBuilder();
        ArrayList<String> commands = builder.addInput(path).validateCommand().buildCommands();
        ProcessResult pr = ProcessRunner.run(commands, 500, isCanceled);
        if (pr.isSuccess()) {
            logger.debug("Media validation succeeded for '{}'", path);
        } else {
            logger.debug("Media validation failed for '{}': exitCode={}, error={}", path, pr.exitCode(), pr.stderr());
        }
        return pr.isSuccess();
    }

    public Optional<String> checkFFmpegVersion(BooleanSupplier isCanceled) {
        logger.debug("Checking FFmpeg version...");
        FFmpegCommandBuilder builder = new FFmpegCommandBuilder();
        ArrayList<String> commands = builder.versionInfo().buildCommands();
        ProcessResult pr = ProcessRunner.run(commands, 500, isCanceled);
        if (!pr.isSuccess()) {
            logger.debug("FFmpeg version command failed: exitCode={}, error={}", pr.exitCode(), pr.stderr());
        }
        return pr.isSuccess() ? parseVersion(pr.stdout()) : Optional.empty();
    }

    public Optional<String> checkFFprobeVersion(BooleanSupplier isCanceled) {
        logger.debug("Checking FFprobe version...");
        FFmpegCommandBuilder builder = new FFmpegCommandBuilder();

        ArrayList<String> commands = builder.setFFprobeExecutable()
                .versionInfo()
                .buildCommands();
        ProcessResult pr = ProcessRunner.run(commands, 500, isCanceled);
        if (!pr.isSuccess()) {
            logger.debug("FFprobe version command failed: exitCode={}, error={}", pr.exitCode(), pr.stderr());
        }
        return pr.isSuccess() ? parseVersion(pr.stdout()) : Optional.empty();
    }

    public boolean isAvailable() {
        boolean available = checkFFmpegVersion(() -> false).isPresent() && checkFFprobeVersion(() -> false).isPresent();
        logger.debug("FFmpeg/FFprobe availability: {}", available);
        return available;
    }

    private Optional<String> parseVersion(String ffmpegOutput) {
        if (ffmpegOutput == null || ffmpegOutput.isBlank()) {
            logger.debug("Cannot parse version: process output was empty or null");
            return Optional.empty();
        }

        Matcher matcher = VERSION_PATTERN.matcher(ffmpegOutput);
        if (matcher.find()) {
            String version = matcher.group(1);
            logger.debug("Parsed version: {}", version);
            return Optional.of(version);
        }
        logger.debug("Failed to parse version matching pattern from output: {}", ffmpegOutput);
        return Optional.empty();
    }
}
