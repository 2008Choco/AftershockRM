package wtf.choco.aftershock.util;

@FunctionalInterface
public interface TriConsumer<T, U, V> {

    public void accept(T t, U u, V v);

}
