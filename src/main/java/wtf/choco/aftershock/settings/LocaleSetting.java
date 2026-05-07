package wtf.choco.aftershock.settings;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.util.Locale;

public final class LocaleSetting extends Setting<Locale, ObjectProperty<Locale>> {

    public LocaleSetting(String key, Locale initialValue) {
        super(key, SimpleObjectProperty::new, initialValue);
    }

    @Override
    public Locale deserialize(String input) {
        return Locale.of(input);
    }

}
