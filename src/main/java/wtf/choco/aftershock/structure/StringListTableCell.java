package wtf.choco.aftershock.structure;

import java.util.List;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

public class StringListTableCell<T> extends TableCell<ReplayEntry, List<T>> {

    private final String emptyDisplayValue;

    private StringListTableCell(String emptyDisplayValue) {
        this.emptyDisplayValue = emptyDisplayValue;
    }

    @Override
    protected void updateItem(List<T> item, boolean empty) {
        if (item == getItem()) {
            return;
        }

        super.updateItem(item, empty);

        if (empty) {
            this.setGraphic(null);
            this.setText(null);
            return;
        }

        this.setGraphic(null); // TODO: Better format than List#toString() --v
        this.setText((item != null && !item.isEmpty()) ? item.toString() : emptyDisplayValue);
    }

    public static <T> Callback<TableColumn<ReplayEntry, List<T>>, TableCell<ReplayEntry, List<T>>> getFactoryCallback(String emptyDisplayValue) {
        return ignore -> new StringListTableCell<>(emptyDisplayValue);
    }

    public static <T> Callback<TableColumn<ReplayEntry, List<T>>, TableCell<ReplayEntry, List<T>>> getFactoryCallback() {
        return getFactoryCallback(null);
    }

}
