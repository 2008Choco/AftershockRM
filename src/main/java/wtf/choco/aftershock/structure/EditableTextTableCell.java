package wtf.choco.aftershock.structure;

import java.util.Objects;

import javafx.scene.Node;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.util.Callback;

public class EditableTextTableCell<S> extends TableCell<S, String> {

    private final TextField commentEditor;
    private final String emptyText;

    public EditableTextTableCell(String emptyText) {
        this.emptyText = emptyText;
        this.commentEditor = new TextField();
        this.commentEditor.prefWidthProperty().bind(widthProperty());

        this.setOnMouseClicked(e -> {
            Node currentGraphic = getGraphic();
            if (!isEditable() || currentGraphic == commentEditor || e.getClickCount() != 2) {
                return;
            }

            this.getTableView().edit(getTableRow().getIndex(), getTableColumn());

            String text = getText();
            this.commentEditor.setText(Objects.equals(emptyText, text) ? "" : text);
            this.setGraphic(commentEditor);
            this.setText(null);
        });

        // FIXME: These keys... work? Enter randomly throws an exception and escape just sets the text to empty
        this.commentEditor.setOnKeyPressed(e -> {
            KeyCode key = e.getCode();
            if (key != KeyCode.ENTER && key != KeyCode.ESCAPE) {
                return;
            }

            this.setGraphic(null);
            this.commitEdit(safe(commentEditor.getText()));
        });

        this.commentEditor.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                this.setGraphic(null);
                this.commitEdit(safe(commentEditor.getText()));
            }
        });
    }

    @Override
    protected void updateItem(String item, boolean empty) {
        if (item == getItem()) {
            return;
        }

        super.updateItem(item, empty);

        if (empty) {
            this.setGraphic(null);
            this.setText(null);
            return;
        }

        if (getGraphic() == null) {
            this.setText((item != null && !item.isBlank()) ? item : emptyText);
        }
    }

    private String safe(String string) {
        return (string != null && !string.isBlank()) ? string.strip() : null;
    }

    public static <S> Callback<TableColumn<S, String>, TableCell<S, String>> getFactoryCallback(String emptyText) {
        return ignore -> new EditableTextTableCell<>(emptyText);
    }

    public static <S> Callback<TableColumn<S, String>, TableCell<S, String>> getFactoryCallback() {
        return getFactoryCallback(null);
    }

}
