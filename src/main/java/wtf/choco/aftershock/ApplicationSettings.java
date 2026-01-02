package wtf.choco.aftershock;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public final class ApplicationSettings {

    private static final Properties PROPERTIES = new Properties();

    private static final Map<String, Setting> SETTING_BY_KEY = new HashMap<>();

    public static final Setting REPLAY_DIRECTORY = createSetting("replay_directory");
    public static final Setting ROCKETRP_PATH = createSetting("rocketrp_path", getCanonicalPath(App.getInstance().getInstallDirectory()) + "\\RocketRP\\RocketRP.CLI.exe");
    public static final Setting REPLAY_EDITOR_PATH = createSetting("replay_editor_path");
    public static final Setting LOCALE = createSetting("locale_code", "en_US");

    public static void init(App app) throws IOException {
        Path path = getPropertiesFilePath(app);

        if (Files.exists(path)) {
            app.getLogger().info("Reading properties from app.properties file...");
            PROPERTIES.load(Files.newBufferedReader(path, StandardCharsets.UTF_8));
            PROPERTIES.forEach((key, value) -> {
                Setting setting = SETTING_BY_KEY.get(key.toString());
                if (setting != null) {
                    setting.set(value.toString());
                }
            });
        } else {
            app.getLogger().info("No app.properties file exists. Creating a new one with default settings...");
            save(app);
        }

        app.getLogger().info("Done!");
    }

    public static void save(App app) throws IOException {
        PROPERTIES.store(Files.newBufferedWriter(getPropertiesFilePath(app), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.WRITE), null);
    }

    private static Path getPropertiesFilePath(App app) {
        return app.getInstallDirectory().toPath().resolve("app.properties");
    }

    private static String getCanonicalPath(File file) {
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            return file.getAbsolutePath();
        }
    }

    public static final class Setting {

        private final StringProperty property;

        private Setting(String key, String defaultValue) {
            this.property = new SimpleStringProperty(defaultValue);
            this.property.addListener((_, _, newValue) -> PROPERTIES.setProperty(key, newValue));
        }

        private Setting(String key) {
            this(key, "");
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
