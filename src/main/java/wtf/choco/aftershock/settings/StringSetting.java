package wtf.choco.aftershock.settings;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public final class StringSetting extends Setting<String, StringProperty> {

    public StringSetting(String key, String initialValue) {
        super(key, SimpleStringProperty::new, initialValue);
    }

    public StringSetting(String key) {
        this(key, "");
    }

    @Override
    public String deserialize(String input) {
        return input;
    }

}
