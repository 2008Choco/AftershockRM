package wtf.choco.aftershock.structure;

import java.util.List;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

public class StringListTableCell extends TableCell<ReplayEntry, List<String>> {

    private StringListTableCell() { }

    @Override
    protected void updateItem(List<String> item, boolean empty) {
        if (item == getItem()) {
            return;
        }

        super.updateItem(item, empty);
        this.setGraphic(null); // TODO: Better format than List#toString() --v
        this.setText((item != null && !empty && !item.isEmpty()) ? item.toString() : "None");
    }

    public static Callback<TableColumn<ReplayEntry, List<String>>, TableCell<ReplayEntry, List<String>>> getFactoryCallback() {
        return ignore -> new StringListTableCell();
    }

}
