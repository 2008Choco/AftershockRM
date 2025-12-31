package wtf.choco.aftershock.util.function;

@FunctionalInterface
public interface TriConsumer<T, U, V> {

    public void accept(T t, U u, V v);

}
