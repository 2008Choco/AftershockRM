package wtf.choco.aftershock.util.function;

import java.util.function.Consumer;

@FunctionalInterface
public interface ThrowingConsumer<T> {

    public void accept(T t) throws Exception;

    public static <T> Consumer<T> unwrap(ThrowingConsumer<T> consumer) {
        return t -> {
            try {
                consumer.accept(t);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

}
