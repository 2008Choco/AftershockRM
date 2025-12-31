package wtf.choco.aftershock;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public final class ApplicationSettings {

    private static final List<Setting> SETTINGS = new ArrayList<>();

    public static final Setting REPLAY_DIRECTORY = createSetting("replay_directory");
    public static final Setting ROCKETRP_PATH = createSetting("rocketrp_path", getCanonicalPath(App.getInstance().getInstallDirectory()) + "\\RocketRP\\RocketRP.CLI.exe");
    public static final Setting REPLAY_EDITOR_PATH = createSetting("replay_editor_path");
    public static final Setting LOCALE = createSetting("locale_code", "en_US");

    private final Path localFilePath;
    private final Properties properties;

    ApplicationSettings(App app) {
        this.properties = new Properties();
        SETTINGS.forEach(setting -> setting.saveTo(properties));

        this.localFilePath = app.getInstallDirectory().toPath().resolve("app.properties");

        try {
            Files.createFile(localFilePath);
            this.writeToFile();
        } catch (FileAlreadyExistsException e) {
            app.getLogger().info("Found app.properties file. Loading...");
            this.readFromFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String get(Setting setting) {
        return properties.computeIfAbsent(setting.key(), _ -> setting.defaultValue()).toString();
    }

    public void set(Setting setting, String value) {
        this.properties.setProperty(setting.key(), value);
    }

    public void readFromFile() {
        try {
            this.properties.load(Files.newBufferedReader(localFilePath, StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeToFile() {
        try {
            this.properties.store(Files.newBufferedWriter(localFilePath, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.WRITE), null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getCanonicalPath(File file) {
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static final class Setting {

        private final String key, defaultValue;

        private Setting(String key, String defaultValue) {
            this.key = key;
            this.defaultValue = defaultValue;
            SETTINGS.add(this);
        }

        public String key() {
            return key;
        }

        public String defaultValue() {
            return defaultValue;
        }

        private void saveTo(Properties properties) {
            properties.put(key, defaultValue);
        }

    }

    private static Setting createSetting(String key, String defaultValue) {
        return new Setting(key, defaultValue);
    }

    private static Setting createSetting(String key) {
        return createSetting(key, "");
    }

}
