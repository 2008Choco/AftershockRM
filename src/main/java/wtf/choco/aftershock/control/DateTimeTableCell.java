package wtf.choco.aftershock.control;

import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;
import wtf.choco.aftershock.structure.ReplayEntry;

import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

public final class DateTimeTableCell<T extends TemporalAccessor> extends TableCell<ReplayEntry, T> {

    private final ObservableValue<DateTimeFormatter> formatter;

    public DateTimeTableCell(ObservableValue<DateTimeFormatter> formatter) {
        this.formatter = formatter;
    }

    @Override
    protected void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);
        this.setText(empty ? "" : formatter.getValue().format(item));
    }

    public static <T extends TemporalAccessor> Callback<TableColumn<ReplayEntry, T>, TableCell<ReplayEntry, T>> getFactoryCallback(ObservableValue<DateTimeFormatter> formatter) {
        return _ -> new DateTimeTableCell<>(formatter);
    }

}
