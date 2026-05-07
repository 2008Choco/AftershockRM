package wtf.choco.aftershock.settings;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public final class BooleanSetting extends Setting<Boolean, BooleanProperty> {

    public BooleanSetting(String key, boolean initialValue) {
        super(key, SimpleBooleanProperty::new, initialValue);
    }

    public BooleanSetting(String key) {
        this(key, false);
    }

    public void set(boolean value) {
        this.property().set(value);
    }

    public boolean get() {
        return property().get();
    }

    @Override
    public Boolean deserialize(String input) {
        return Boolean.parseBoolean(input);
    }

}
