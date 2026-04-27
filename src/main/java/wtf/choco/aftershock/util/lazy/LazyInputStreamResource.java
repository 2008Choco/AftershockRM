package wtf.choco.aftershock.util.lazy;

import java.io.InputStream;
import java.util.function.Function;

final class LazyInputStreamResource<T> extends LazyResource<InputStream, T> {

    LazyInputStreamResource(Class<?> resourceClass, String resourcePath, Function<InputStream, T> resourceMapper) {
        super(resourceClass, resourcePath, resourceMapper);
    }

    @Override
    protected InputStream getResource(Class<?> resourceClass, String resourcePath) {
        return resourceClass.getResourceAsStream(resourcePath);
    }

}
