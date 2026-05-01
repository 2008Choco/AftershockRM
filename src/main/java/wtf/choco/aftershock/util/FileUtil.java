package wtf.choco.aftershock.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FileUtil {

    private FileUtil() { }

    public static void createFileIfDoesntExist(Path path) {
        if (Files.exists(path)) {
            return;
        }

        try {
            Files.createFile(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void createDirectoryIfDoesntExist(Path path) {
        if (Files.isDirectory(path)) {
            return;
        }

        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            e.printStackTrace();
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

    public static Path changeExtension(Path path, String newExtension) {
        String extension = getExtension(path);
        String fileName = path.getFileName().toString();
        fileName = fileName.replace(extension, newExtension);

        // Special case for small path names
        if (path.getNameCount() == 1) {
            return Path.of(fileName);
        }

        return path.getParent().resolve(fileName);
    }

}
