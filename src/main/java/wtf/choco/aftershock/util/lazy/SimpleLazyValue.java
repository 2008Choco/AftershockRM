package wtf.choco.aftershock.util.lazy;

import java.util.function.Supplier;

final class SimpleLazyValue<T> implements LazyValue<T> {

    private T value;

    private final Supplier<T> valueSupplier;

    SimpleLazyValue(Supplier<T> valueSupplier) {
        this.valueSupplier = valueSupplier;
    }

    public T get() {
        return (value == null) ? (value = valueSupplier.get()) : value;
    }

}
