package wtf.choco.aftershock.util.lazy;

import java.net.URL;
import java.util.function.Function;

final class LazyURLResource<T> extends LazyResource<URL, T> {

    LazyURLResource(Class<?> resourceClass, String resourcePath, Function<URL, T> resourceMapper) {
        super(resourceClass, resourcePath, resourceMapper);
    }

    @Override
    protected URL getResource(Class<?> resourceClass, String resourcePath) {
        return resourceClass.getResource(resourcePath);
    }

}
