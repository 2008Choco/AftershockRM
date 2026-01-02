package wtf.choco.aftershock.util.function;

import java.util.function.Consumer;
import java.util.function.Function;

@FunctionalInterface
public interface ThrowingConsumer<T> {

    public void accept(T t) throws Exception;

    public default Consumer<T> toConsumer(Function<Throwable, RuntimeException> errorWrapper) {
        return t -> {
            try {
                this.accept(t);
            } catch (Throwable e) {
                if (e instanceof RuntimeException exception) {
                    throw exception;
                }

                if (e instanceof Error error) {
                    throw error;
                }

                throw errorWrapper.apply(e);
            }
        };
    }

    public default Consumer<T> toConsumer() {
        return toConsumer(RuntimeException::new);
    }

}
