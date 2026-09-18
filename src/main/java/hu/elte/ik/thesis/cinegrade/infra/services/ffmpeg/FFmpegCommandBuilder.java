package hu.elte.ik.thesis.cinegrade.infra.services.ffmpeg;

import hu.elte.ik.thesis.cinegrade.infra.process.NativeBinaryLocator;
import hu.elte.ik.thesis.cinegrade.infra.services.CommandBuilder;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FFmpegCommandBuilder implements CommandBuilder {

    private final String FFPROBE_EXECUTABLE = NativeBinaryLocator.getExecutablePath("ffprobe").toString();
    private final String FFMPEG_EXECUTABLE = NativeBinaryLocator.getExecutablePath("ffmpeg").toString();

    private String executable = FFMPEG_EXECUTABLE;
    private final ArrayList<String> args = new ArrayList<>();

    private void setExecutablePath(String path) {
        executable = path;
    }

    public FFmpegCommandBuilder versionInfo() {
        args.add("-version");
        return this;
    }

    // region VIDEO RELATED OPTIONS
    public FFmpegCommandBuilder addVideoCodec(String codec) {
        if (codec != null) {
            args.add("-c:v");
            args.add(codec);
        }
        return this;
    }

    public FFmpegCommandBuilder addAudioCodec(String codec) {
        if (codec != null) {
            args.add("-c:a");
            args.add(codec);
        }
        return this;
    }

    public FFmpegCommandBuilder addVideoBitrate(String bitrate) {
        if (bitrate != null && !bitrate.isBlank()) {
            args.add("-b:v");
            args.add(bitrate.endsWith("k") ? bitrate : bitrate + "k");
        }
        return this;
    }

    public FFmpegCommandBuilder addAudioBitrate(String bitrate) {
        if (bitrate != null && !bitrate.isBlank()) {
            args.add("-b:a");
            args.add(bitrate.endsWith("k") ? bitrate : bitrate + "k");
        }
        return this;
    }

    public FFmpegCommandBuilder addResolution(String resolution) {
        if (resolution != null && !resolution.isBlank()) {
            switch (resolution.toLowerCase()) {
                case "4k" -> args.addAll(List.of("-s", "3840x2160"));
                case "1080p" -> args.addAll(List.of("-s", "1920x1080"));
                case "720p" -> args.addAll(List.of("-s", "1280x720"));
                case "480p" -> args.addAll(List.of("-s", "854x480"));
                case "auto" -> {
                    return this;
                }
                default -> {
                    if (resolution.matches("\\d{3,5}x\\d{3,5}")) {
                        args.add("-s");
                        args.add(resolution);
                    }
                }
            }
        }
        return this;
    }

    public FFmpegCommandBuilder addFramerate(String framerate) {
        if (framerate != null && !framerate.isBlank() && !framerate.equalsIgnoreCase("auto")) {
            args.add("-r");
            args.add(framerate);
        }
        return this;
    }

    public FFmpegCommandBuilder addSampleRate(String rate) {
        if (rate != null && !rate.isBlank()) {
            args.add("-ar");
            args.add(rate);
        }
        return this;
    }

    public FFmpegCommandBuilder addAudioEnabled(boolean keepAudio) {
        if (!keepAudio) {
            args.add("-an");
        }
        return this;
    }

    public FFmpegCommandBuilder addOutputFormat(String format) {
        if (format != null && !format.isBlank()) {
            args.add("-f");
            args.add(format.replace(".", ""));
        }
        return this;
    }

    public FFmpegCommandBuilder addTrimInterval(String startTime, String endTime) {
        if (startTime != null && !startTime.isBlank()) {
            args.add("-ss");
            args.add(startTime);
        }
        if (endTime != null && !endTime.isBlank()) {
            args.add("-to");
            args.add(endTime);
        }
        return this;
    }
    // endregion

    // region IMAGE RELATED OPTIONS
    // For JPEG / WebP quality
    public FFmpegCommandBuilder addImageQuality(int quality) {
        args.add("-q:v");
        args.add(String.valueOf(quality));
        return this;
    }

    // For PNG compression level (0-9)
    public FFmpegCommandBuilder addPngCompression(int level) {
        args.add("-compression_level");
        args.add(String.valueOf(Math.clamp(level, 0, 9)));
        return this;
    }

    // For WebP lossless toggle
    public FFmpegCommandBuilder setLossless(boolean lossless) {
        args.add("-lossless");
        args.add(lossless ? "1" : "0");
        return this;
    }

    // Resampling / scaling
    public FFmpegCommandBuilder addScale(int width, int height, boolean keepAspect) {
        if (keepAspect) {
            // e.g. scale=1920:-1 (scales width to 1920, keeps proportional height)
            args.addAll(List.of("-vf", String.format("scale=%d:%d:flags=lanczos", width, height)));
        } else {
            args.addAll(List.of("-vf", String.format("scale=%d:%d:flags=lanczos", width, height)));
        }
        return this;
    }

    // Strip metadata
    public FFmpegCommandBuilder setStripMetadata(boolean strip) {
        args.add("-map_metadata");
        args.add(strip ? "-1" : "0");
        return this;
    }

    // Apply 3D LUT filter
    public FFmpegCommandBuilder addLut3D(Path lutFile) {
        args.add("-vf");
        String path = lutFile.toAbsolutePath().toString().replace("\\", "/");
        args.add("lut3d='" + path + "'");
        return this;
    }
    // endregion

    // region OTHER OPTIONS
    // Validate command using ffprobe to check if the input file is valid
    public FFmpegCommandBuilder validateCommand() {
        setExecutablePath(FFPROBE_EXECUTABLE);
        args.addAll(List.of(
                "-hide_banner",
                "-nostdin",
                "-loglevel", "error",
                "-probesize", "5M",
                "-analyzeduration", "0",
                "-count_frames",
                "-select_streams", "v:0",
                "-show_entries", "stream=codec_type,nb_read_frames",
                "-of", "default=noprint_wrappers=1:nokey=1"
        ));
        return this;
    }

    public FFmpegCommandBuilder setFFprobeExecutable() {
        setExecutablePath(FFPROBE_EXECUTABLE);
        return this;
    }

    public FFmpegCommandBuilder addInput(Path input) {
        args.add("-i");
        args.add(input.toFile().getAbsolutePath());
        return this;
    }

    public FFmpegCommandBuilder addOutput(Path output) {
        args.add("-o");
        args.add(output.toFile().getAbsolutePath());
        return this;
    }

    public FFmpegCommandBuilder addCommandLine(String command) {
        if (command != null && !command.isBlank()) {
            args.add(command);
        }
        return this;
    }

    public FFmpegCommandBuilder addOverwrite(boolean overwrite) {
        if (overwrite) {
            args.add("-y");
        }
        return this;
    }

    public FFmpegCommandBuilder setLogLevel(String level) {
        args.add("-v");
        args.add(level); // e.g. "error", "warning"
        return this;
    }
    // endregion

    public ArrayList<String> buildCommands() {
        ArrayList<String> finalCommands = new ArrayList<>();
        finalCommands.add(executable);
        finalCommands.addAll(args);
        return finalCommands;
    }
}
