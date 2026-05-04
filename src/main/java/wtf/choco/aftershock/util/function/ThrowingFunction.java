package wtf.choco.aftershock.util.function;

import java.util.function.Function;

@FunctionalInterface
public interface ThrowingFunction<T, R> {

    public R apply(T t) throws Exception;

    public static <T, R> Function<T, R> unwrap(ThrowingFunction<T, R> function) {
        return t -> {
            try {
                return function.apply(t);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

}
