package wtf.choco.aftershock.structure;

import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TableCell;

public class BooleanCheckboxCell<T> extends TableCell<T, Boolean> {

    private final CheckBox checkBox;

    public BooleanCheckboxCell() {
        this.checkBox = new CheckBox();
        this.checkBox.setDisable(true);
        this.checkBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (isEditing()) {
                this.commitEdit(newValue == null ? false : newValue);
            }
        });

        this.setGraphic(checkBox);
        this.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        this.setEditable(true);
    }

    @Override
    public void startEdit() {
        super.startEdit();
        if (isEmpty()) {
            return;
        }

        this.checkBox.setDisable(false);
        this.checkBox.requestFocus();
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();
        this.checkBox.setDisable(true);
    }

    @Override
    public void commitEdit(Boolean value) {
        super.commitEdit(value);
        this.checkBox.setDisable(true);
    }

    @Override
    public void updateItem(Boolean item, boolean empty) {
        super.updateItem(item, empty);
        if (!isEmpty()) {
            this.checkBox.setSelected(item);
        }
    }

}
