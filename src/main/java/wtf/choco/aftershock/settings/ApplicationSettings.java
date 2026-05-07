package wtf.choco.aftershock.settings;

import javafx.beans.property.Property;
import wtf.choco.aftershock.App;
import wtf.choco.aftershock.files.AftershockFileStructure;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

public final class ApplicationSettings {

    private static final Properties PROPERTIES = new Properties();
    private static final Map<String, Setting<?, ?>> SETTING_BY_KEY = new HashMap<>();

    public static final PathSetting REPLAY_DIRECTORY = register(new PathSetting("replay_directory"));
    public static final PathSetting REPLAY_EDITOR_PATH = register(new PathSetting("replay_editor_path"));
    public static final EnumSetting<ReplayDateFormat> REPLAY_DATE_FORMAT = register(new EnumSetting<>("replay_date_format", ReplayDateFormat.class, ReplayDateFormat.DAY_MONTH_YEAR_SHORT));
    public static final BooleanSetting REPLAY_24_HOUR_TIME = register(new BooleanSetting("replay_24_hour_time", true));
    public static final LocaleSetting LOCALE = register(new LocaleSetting("locale_code", Locale.of("en_US")));

    private ApplicationSettings() { }

    public static void init(AftershockFileStructure fileStructure) throws IOException {
        Path propertiesFilePath = fileStructure.propertiesFile();
        if (!Files.isRegularFile(propertiesFilePath)) {
            App.LOGGER.info("No app.properties file exists. Creating a new one with default settings...");
            save(fileStructure);
            return;
        }

        PROPERTIES.load(Files.newBufferedReader(propertiesFilePath, StandardCharsets.UTF_8));
        PROPERTIES.forEach((key, value) -> {
            Setting<?, ?> setting = SETTING_BY_KEY.get(key.toString());
            if (setting != null) {
                setting.deserializeAndSet(value.toString());
            }
        });
    }

    public static void save(AftershockFileStructure fileStructure) throws IOException {
        PROPERTIES.store(Files.newBufferedWriter(fileStructure.propertiesFile(), StandardCharsets.UTF_8), null);
    }

    private static <T, V extends Property<T>, S extends Setting<T, V>> S register(S setting) {
        SETTING_BY_KEY.put(setting.getKey(), setting);
        setting.property().addListener((_, _, newValue) -> PROPERTIES.setProperty(setting.getKey(), setting.serialize(newValue)));
        return setting;
    }

}
