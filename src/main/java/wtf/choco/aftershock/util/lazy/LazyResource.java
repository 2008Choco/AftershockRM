package wtf.choco.aftershock.util.lazy;

import java.util.MissingResourceException;
import java.util.function.Function;

abstract class LazyResource<T, R> implements LazyValue<R> {

    private R value;

    private final Class<?> resourceClass;
    private final String resourcePath;
    private final Function<T, R> resourceMapper;

    LazyResource(Class<?> resourceClass, String resourcePath, Function<T, R> resourceMapper) {
        this.resourceClass = resourceClass;
        this.resourcePath = resourcePath;
        this.resourceMapper = resourceMapper;
    }

    protected abstract T getResource(Class<?> resourceClass, String resourcePath);

    @Override
    public R get() {
        if (value == null) {
            T resource = getResource(resourceClass, resourcePath);
            if (resource == null) {
                throw new MissingResourceException("Could not find resource for lazy loading", resourceClass.getName(), resourcePath);
            }

            this.value = resourceMapper.apply(resource);
        }

        return value;
    }

}
