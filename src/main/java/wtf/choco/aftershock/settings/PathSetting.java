package wtf.choco.aftershock.settings;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.nio.file.Path;

public final class PathSetting extends Setting<Path, ObjectProperty<Path>> {

    public PathSetting(String key, Path initialValue) {
        super(key, SimpleObjectProperty::new, initialValue);
    }

    public PathSetting(String key) {
        this(key, null);
    }

    @Override
    public String serialize(Path value) {
        return (value != null) ? value.toAbsolutePath().toString() : "";
    }

    @Override
    public Path deserialize(String input) {
        return Path.of(input);
    }

}
