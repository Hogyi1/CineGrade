package hu.elte.ik.thesis.cinegrade.infra.services.exiftool;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import hu.elte.ik.thesis.cinegrade.domain.catalog.PhotoMetadata;
import hu.elte.ik.thesis.cinegrade.domain.enums.ExifTag.*;
import hu.elte.ik.thesis.cinegrade.domain.results.ProcessResult;
import hu.elte.ik.thesis.cinegrade.infra.process.ProcessRunner;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Locale;
import java.util.function.BooleanSupplier;

import static hu.elte.ik.thesis.cinegrade.domain.enums.ExifTag.*;

public class ExifToolService {

    public PhotoMetadata readMetaData(Path path, BooleanSupplier isCanceled) {

        ExifToolCommandBuilder builder = new ExifToolCommandBuilder();
        String colorProfile = "";

        ArrayList<String> commands = builder.addTags(MAKE, MODEL, F_NUMBER, EXPOSURE_TIME, ISO, FOCAL_LENGTH, DATE_TIME_ORIGINAL, COLOR_MODE)
                .asJson()
                .withUtf8()
                .fastMode()
                .addTarget(path)
                .buildCommands();
        ProcessResult pr = ProcessRunner.run(commands, 500, isCanceled);

        JsonObject jsonObject = new Gson().fromJson(pr.stdout(), JsonObject.class);

        for (var entry : jsonObject.asMap().entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();

            boolean hasColorProfile = COLOR_MODE.getTagName().toLowerCase().contains(key.toLowerCase());
            if (hasColorProfile && !(value == null || value.isJsonNull())) {
                colorProfile = value.getAsString();
            }
        }

        PhotoMetadata photoMetadata = new Gson().fromJson(pr.stdout(), PhotoMetadata.class);
        photoMetadata.setColorStyle(colorProfile);
        return photoMetadata;
    }

    public boolean checkExifToolExists(BooleanSupplier isCanceled) {

        ExifToolCommandBuilder builder = new ExifToolCommandBuilder();

        ArrayList<String> commands = builder.versionInfo().buildCommands();
        ProcessResult pr = ProcessRunner.run(commands, 500, isCanceled);
        return pr.isSuccess();
    }
}
