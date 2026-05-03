package wtf.choco.aftershock;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import wtf.choco.aftershock.files.AftershockFileStructure;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public final class ApplicationSettings {

    private static final Properties PROPERTIES = new Properties();

    private static final Map<String, Setting> SETTING_BY_KEY = new HashMap<>();

    public static final Setting REPLAY_DIRECTORY = createSetting("replay_directory");
    public static final Setting REPLAY_EDITOR_PATH = createSetting("replay_editor_path");
    public static final Setting LOCALE = createSetting("locale_code", "en_US");

    public static void init(App app, AftershockFileStructure fileStructure) throws IOException {
        Path propertiesFilePath = fileStructure.propertiesFile();
        if (Files.exists(propertiesFilePath)) {
            app.getLogger().info("Reading properties from app.properties file...");
            PROPERTIES.load(Files.newBufferedReader(propertiesFilePath, StandardCharsets.UTF_8));
            PROPERTIES.forEach((key, value) -> {
                Setting setting = SETTING_BY_KEY.get(key.toString());
                if (setting != null) {
                    setting.set(value.toString());
                }
            });
        } else {
            app.getLogger().info("No app.properties file exists. Creating a new one with default settings...");
            save(fileStructure);
        }

        app.getLogger().info("Done!");
    }

    public static void save(AftershockFileStructure fileStructure) throws IOException {
        PROPERTIES.store(Files.newBufferedWriter(fileStructure.propertiesFile(), StandardCharsets.UTF_8), null);
    }

    public static final class Setting {

        private final StringProperty property;

        private Setting(String key, String defaultValue) {
            this.property = new SimpleStringProperty(defaultValue);
            this.property.addListener((_, _, newValue) -> PROPERTIES.setProperty(key, newValue));
        }

        public void set(String value) {
            this.property().set(value.strip());
        }

        public String get() {
            return property().get();
        }

        public StringProperty property() {
            return property;
        }

    }

    private static Setting createSetting(String key, String defaultValue) {
        Setting setting = new Setting(key, defaultValue);
        SETTING_BY_KEY.put(key, setting);
        return setting;
    }

    private static Setting createSetting(String key) {
        return createSetting(key, "");
    }

}
