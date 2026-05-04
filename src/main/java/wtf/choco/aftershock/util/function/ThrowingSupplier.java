package wtf.choco.aftershock.util.function;

import java.util.function.Supplier;

@FunctionalInterface
public interface ThrowingSupplier<T> {

    public T get() throws Exception;

    public static <T> Supplier<T> unwrap(ThrowingSupplier<T> supplier) {
        return () -> {
            try {
                return supplier.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

}
