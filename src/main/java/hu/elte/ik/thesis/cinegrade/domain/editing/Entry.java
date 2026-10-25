package hu.elte.ik.thesis.cinegrade.domain.editing;

import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.util.Locale;

public abstract class Entry {

    @Getter
    @Setter
    protected File file;

    public Entry(File file) {
        this.file = file;
    }

    public final String getBaseKey() {
        int dotIndex = file.getAbsolutePath().lastIndexOf(".");
        int separatorIndex = file.getAbsolutePath().lastIndexOf(File.separator);
        return dotIndex > separatorIndex ? file.getAbsolutePath().substring(0, dotIndex).toLowerCase(Locale.ROOT) : file.getAbsolutePath().toLowerCase(Locale.ROOT);
    }

    public final String getFileNameWithoutExtension() {
        int dotIndex = file.getName().lastIndexOf(".");
        return file.getName().substring(0, dotIndex).toLowerCase(Locale.ROOT);
    }
}
