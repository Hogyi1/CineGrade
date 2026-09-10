package hu.elte.ik.thesis.cinegrade.infra.services.ffmpeg;

import hu.elte.ik.thesis.cinegrade.domain.results.ProcessResult;
import hu.elte.ik.thesis.cinegrade.infra.process.ProcessRunner;
import hu.elte.ik.thesis.cinegrade.infra.services.exiftool.ExifToolCommandBuilder;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.function.BooleanSupplier;

public class FFmpegService {

    public boolean validateInput(Path path, BooleanSupplier isCanceled) {
        FFmpegCommandBuilder builder = new FFmpegCommandBuilder();
        ArrayList<String> commands = builder.addInput(path).validateCommand().buildCommands();
        ProcessResult pr = ProcessRunner.run(commands, 500, isCanceled);
        return pr.isSuccess();
    }

    public boolean checkFFmpegExists(BooleanSupplier isCanceled) {
        FFmpegCommandBuilder builder = new FFmpegCommandBuilder();
        ArrayList<String> commands = builder.versionInfo().buildCommands();
        ProcessResult pr = ProcessRunner.run(commands, 500, isCanceled);
        return pr.isSuccess();
    }

    public boolean checkFFprobeExists(BooleanSupplier isCanceled) {

        FFmpegCommandBuilder builder = new FFmpegCommandBuilder();

        ArrayList<String> commands = builder.setFFprobeExecutable()
                .versionInfo()
                .buildCommands();
        ProcessResult pr = ProcessRunner.run(commands, 500, isCanceled);
        return pr.isSuccess();
    }
}
