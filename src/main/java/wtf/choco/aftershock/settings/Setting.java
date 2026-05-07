package wtf.choco.aftershock.settings;

import javafx.beans.property.Property;
import javafx.util.StringConverter;

import java.util.function.Function;

public abstract class Setting<T, V extends Property<T>> {

    private Converter converter;

    private final String key;
    private final V property;
    private final T initialValue;

    protected Setting(String key, Function<T, V> propertyConstructor, T initialValue) {
        this.key = key;
        this.property = propertyConstructor.apply(initialValue);
        this.initialValue = initialValue;
    }

    public String getKey() {
        return key;
    }

    public V property() {
        return property;
    }

    public void setValue(T value) {
        this.property.setValue(value);
    }

    public T getValue() {
        return property.getValue();
    }

    public T getInitialValue() {
        return initialValue;
    }

    public String serialize(T value) {
        return (value != null) ? value.toString() : "";
    }

    public String serializeCurrentValue() {
        return serialize(getValue());
    }

    public abstract T deserialize(String input);

    public void deserializeAndSet(String input) {
        this.setValue(deserialize(input));
    }

    public StringConverter<T> converter() {
        if (converter == null) {
            this.converter = new Converter();
        }

        return converter;
    }

    private final class Converter extends StringConverter<T> {

        @Override
        public String toString(T object) {
            return serialize(object);
        }

        @Override
        public T fromString(String string) {
            return deserialize(string);
        }

    }

}
