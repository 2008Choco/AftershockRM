package wtf.choco.aftershock.util.lazy;

import wtf.choco.aftershock.App;

import java.io.InputStream;
import java.net.URL;
import java.util.function.Function;
import java.util.function.Supplier;

public interface LazyValue<T> {

    public T get();

    public static <T> LazyValue<T> of(Supplier<T> supplier) {
        return new SimpleLazyValue<>(supplier);
    }

    public static <T> LazyValue<T> resource(String resourceName, Function<InputStream, T> resourceMapper) {
        return new LazyInputStreamResource<>(App.class, resourceName, resourceMapper);
    }

    public static <T> LazyValue<T> urlResource(String resourceName, Function<URL, T> resourceMapper) {
        return new LazyURLResource<>(App.class, resourceName, resourceMapper);
    }

    public static LazyValue<URL> urlResource(String resourceName) {
        return urlResource(resourceName, Function.identity());
    }

}
