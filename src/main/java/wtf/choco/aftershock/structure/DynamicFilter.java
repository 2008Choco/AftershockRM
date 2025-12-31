package wtf.choco.aftershock.structure;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class DynamicFilter<T> implements Predicate<T> {

    private String term;
    private BiPredicate<T, String> comparator;

    public DynamicFilter(BiPredicate<T, String> comparator, String term) {
        this.comparator = comparator;
        this.term = term;
    }

    public DynamicFilter(BiPredicate<T, String> comparator) {
        this(comparator, null);
    }

    public void setTerm(String term) {
        this.term = term;
    }

    public boolean isInvalid() {
        return term == null || term.isBlank() || comparator == null;
    }

    @Override
    public boolean test(T t) {
        if (comparator == null || term == null || term.isBlank()) {
            return true;
        }

        return comparator.test(t, term);
    }

}
