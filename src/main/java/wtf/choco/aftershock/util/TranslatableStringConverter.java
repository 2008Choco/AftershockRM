package wtf.choco.aftershock.util;

import javafx.util.StringConverter;
import wtf.choco.aftershock.App;

public final class TranslatableStringConverter<T extends Translatable> extends StringConverter<T> {

    private static final TranslatableStringConverter<? extends Translatable> INSTANCE = new TranslatableStringConverter<>();

    @Override
    public String toString(T value) {
        return App.getInstance().getResources().getString(value.getResourceKey());
    }

    @Override
    public T fromString(String string) {
        throw new UnsupportedOperationException("TranslatableStringConverter does not support fromString operation");
    }

    @SuppressWarnings("unchecked")
    public static <T extends Translatable> TranslatableStringConverter<T> get() {
        return (TranslatableStringConverter<T>) INSTANCE;
    }

}
