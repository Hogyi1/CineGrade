package hu.elte.ik.thesis.cinegrade.infra.utils.file;

import java.io.File;
import java.util.Locale;

public final class FileUtils {

    public static String getFileNameWithoutExtension(File file) {
        if (file == null) {
            return "";
        }
        String name = file.getName();
        int dotIndex = name.lastIndexOf('.');
        return (dotIndex > 0 ? name.substring(0, dotIndex) : name).toLowerCase(Locale.ROOT);
    }

    public static String getBaseKey(File file) {
        if (file == null) {
            return "";
        }
        String path = file.getAbsolutePath();
        int dotIndex = path.lastIndexOf('.');
        int sepIndex = Math.max(path.lastIndexOf(File.separatorChar), path.lastIndexOf('/'));
        return (dotIndex > sepIndex ? path.substring(0, dotIndex) : path).toLowerCase(Locale.ROOT);
    }
}
