package wtf.choco.aftershock;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public final class ApplicationSettings {

    private static final List<Setting> SETTINGS = new ArrayList<>();
    private static final Charset CHARSET = Charset.forName("UTF-8");

    public static final Setting REPLAY_DIRECTORY = Setting.of("replay_directory", "");
    public static final Setting RATTLETRAP_PATH = Setting.of("rattletrap_path", App.getInstance().getInstallDirectory().getAbsolutePath() + "\\Rattletrap\\rattletrap.exe");
    public static final Setting LOCALE = Setting.of("locale_code", "en_US");


    private final Path localFilePath;
    private final Properties properties;

    protected ApplicationSettings(App app) {
        this.properties = new Properties();
        SETTINGS.forEach(s -> properties.put(s.key, s.defaultValue));

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
        return properties.computeIfAbsent(setting.key, k -> setting.defaultValue).toString();
    }

    public void set(Setting setting, String value) {
        this.properties.setProperty(setting.key, value);
    }

    public void readFromFile() {
        try {
            this.properties.load(Files.newBufferedReader(localFilePath, CHARSET));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeToFile() {
        try {
            this.properties.store(Files.newBufferedWriter(localFilePath, CHARSET, StandardOpenOption.CREATE, StandardOpenOption.WRITE), null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static final class Setting {

        private final String key, defaultValue;

        private Setting(String key, String defaultValue) {
            this.key = key;
            this.defaultValue = defaultValue;
            SETTINGS.add(this);
        }

        public String getKey() {
            return key;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        private static Setting of(String key, String defaultValue) {
            return new Setting(key, defaultValue);
        }

    }

}
