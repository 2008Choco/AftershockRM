package wtf.choco.aftershock.settings;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import wtf.choco.aftershock.App;

import java.util.logging.Level;

public final class EnumSetting<E extends Enum<E>> extends Setting<E, ObjectProperty<E>> {

    private final Class<E> enumClass;

    public EnumSetting(String key, Class<E> enumClass, E initialValue) {
        super(key, SimpleObjectProperty::new, initialValue);
        this.enumClass = enumClass;
    }

    @Override
    public E deserialize(String input) {
        try {
            return Enum.valueOf(enumClass, input);
        } catch (IllegalArgumentException e) {
            App.LOGGER.log(Level.WARNING, "Failed to parse enum setting value \"" + input + "\" for setting \"" + getKey() + "\"", e);
        }

        return null;
    }

}
