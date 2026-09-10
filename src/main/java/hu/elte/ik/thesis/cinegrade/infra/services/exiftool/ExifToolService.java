package hu.elte.ik.thesis.cinegrade.infra.services.exiftool;

import com.google.gson.Gson;
import hu.elte.ik.thesis.cinegrade.domain.catalog.PhotoMetadata;
import hu.elte.ik.thesis.cinegrade.domain.enums.ExifTag.*;
import hu.elte.ik.thesis.cinegrade.domain.results.ProcessResult;
import hu.elte.ik.thesis.cinegrade.infra.process.ProcessRunner;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.function.BooleanSupplier;

import static hu.elte.ik.thesis.cinegrade.domain.enums.ExifTag.*;

public class ExifToolService {

    public PhotoMetadata readMetaData(Path path, BooleanSupplier isCanceled) {

        ExifToolCommandBuilder builder = new ExifToolCommandBuilder();

        ArrayList<String> commands = builder.addTags(MAKE, MODEL, F_NUMBER, EXPOSURE_TIME, ISO, FOCAL_LENGTH, DATE_TIME_ORIGINAL, COLOR_MODE)
                .asJson()
                .withUtf8()
                .fastMode()
                .addTarget(path)
                .buildCommands();
        ProcessResult pr = ProcessRunner.run(commands, 500, isCanceled);

        // Ha jobban szétakarom szedni saját értékekre, akkor itt kell megoldani
        return new Gson().fromJson(pr.stdout(), PhotoMetadata.class);
    }

    public boolean checkExifToolExists(BooleanSupplier isCanceled) {

        ExifToolCommandBuilder builder = new ExifToolCommandBuilder();

        ArrayList<String> commands = builder.versionInfo().buildCommands();
        ProcessResult pr = ProcessRunner.run(commands, 500, isCanceled);
        return pr.isSuccess();
    }
}
