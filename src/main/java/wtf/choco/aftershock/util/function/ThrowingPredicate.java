package wtf.choco.aftershock.util.function;

import java.util.function.Predicate;

@FunctionalInterface
public interface ThrowingPredicate<T> {

    public boolean test(T t) throws Exception;

    public static <T> Predicate<T> unwrap(ThrowingPredicate<T> predicate) {
        return t -> {
            try {
                return predicate.test(t);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

}
