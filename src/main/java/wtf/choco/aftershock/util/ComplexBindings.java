package wtf.choco.aftershock.util;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableIntegerValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import java.util.function.Function;

public final class ComplexBindings {

    private ComplexBindings() { }

    public static <E> ObservableIntegerValue createIntegerBindingCountingBooleanProperties(ObservableList<E> collection, Function<E, ObservableBooleanValue> propertyGetter) {
        SimpleIntegerProperty countProperty = new SimpleIntegerProperty();

        ChangeListener<Boolean> booleanPropertyChangeListener = (_, _, newValue) -> countProperty.set(countProperty.get() + (newValue ? 1 : -1));

        int currentCount = 0;
        for (E element : collection) {
            ObservableBooleanValue property = propertyGetter.apply(element);
            if (property.get()) {
                currentCount++;
            }

            property.addListener(booleanPropertyChangeListener);
        }

        countProperty.set(currentCount);

        collection.addListener((ListChangeListener<? super E>) change -> {
            int delta = 0;
            while (change.next()) {
                if (change.wasAdded()) {
                    for (E addedElement : change.getAddedSubList()) {
                        ObservableBooleanValue property = propertyGetter.apply(addedElement);
                        if (property.get()) {
                            delta++;
                        }

                        property.addListener(booleanPropertyChangeListener);
                    }
                }

                if (change.wasRemoved()) {
                    for (E removedElement : change.getRemoved()) {
                        ObservableBooleanValue property = propertyGetter.apply(removedElement);
                        if (property.get()) {
                            delta--;
                        }

                        property.removeListener(booleanPropertyChangeListener);
                    }
                }
            }
            countProperty.set(countProperty.get() + delta);
        });

        return countProperty;
    }

}
