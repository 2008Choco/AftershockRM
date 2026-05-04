package wtf.choco.aftershock.util;

import wtf.choco.aftershock.App;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;

public final class FileUtil {

    private FileUtil() { }

    public static void createDirectoryIfDoesntExist(Path path) {
        if (Files.isDirectory(path)) {
            return;
        }

        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            App.LOGGER.log(Level.SEVERE, "Unable to create directory: " + path, e);
        }
    }

    public static String getExtension(Path path) {
        String fileName = path.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');

        if (dotIndex == -1 || dotIndex == fileName.length() - 1) {
            return "";
        }

        return fileName.substring(dotIndex + 1);
    }

}
