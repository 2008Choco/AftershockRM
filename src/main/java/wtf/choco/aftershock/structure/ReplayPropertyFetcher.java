package wtf.choco.aftershock.structure;

import java.util.function.Function;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.util.Callback;

public class ReplayPropertyFetcher<R> implements Callback<CellDataFeatures<ReplayEntry, R>, ObservableValue<R>> {

    private final Function<ReplayEntry, R> valueRetriever;

    public ReplayPropertyFetcher(Function<ReplayEntry, R> valueRetriever) {
        this.valueRetriever = valueRetriever;
    }

    @Override
    public ObservableValue<R> call(CellDataFeatures<ReplayEntry, R> param) {
        R result = valueRetriever.apply(param.getValue());
        return (result != null) ? new ReadOnlyObjectWrapper<>(result) : null;
    }

}
