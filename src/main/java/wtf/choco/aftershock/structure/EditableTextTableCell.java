package wtf.choco.aftershock.structure;

import javafx.scene.Node;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.util.Callback;

import java.util.Objects;

public class EditableTextTableCell<S> extends TableCell<S, String> {

    private final TextField commentEditor;
    private final String emptyText;

    public EditableTextTableCell(String emptyText) {
        this.emptyText = emptyText;
        this.commentEditor = new TextField();
        this.commentEditor.prefWidthProperty().bind(widthProperty());
        this.commentEditor.getStyleClass().add("comment-editor");

        this.setOnMouseClicked(event -> {
            if (isEmpty()) {
                return;
            }

            Node currentGraphic = getGraphic();
            if (!isEditable() || currentGraphic == commentEditor || event.getClickCount() != 2) {
                return;
            }

            this.getTableView().edit(getTableRow().getIndex(), getTableColumn());

            String text = getText();
            this.commentEditor.setText(Objects.equals(emptyText, text) ? "" : text);
            this.setGraphic(commentEditor);
            this.setText(null);
        });

        // FIXME: These keys... work? Enter randomly throws an exception and escape just sets the text to empty
        this.commentEditor.setOnKeyPressed(event -> {
            KeyCode key = event.getCode();
            if (key != KeyCode.ENTER && key != KeyCode.ESCAPE) {
                return;
            }

            this.setGraphic(null);
            this.commitEdit(safe(commentEditor.getText()));
        });

        this.commentEditor.focusedProperty().addListener((_, _, newValue) -> {
            if (!newValue) {
                this.setGraphic(null);
                this.commitEdit(safe(commentEditor.getText()));
            }
        });
    }

    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);

        if (empty) {
            this.setGraphic(null);
            this.setText("");
            return;
        }

        this.setText(item != null && !item.isBlank() ? item : emptyText);
    }

    private String safe(String string) {
        return (string != null && !string.isBlank()) ? string.strip() : null;
    }

    public static <S> Callback<TableColumn<S, String>, TableCell<S, String>> getFactoryCallback(String emptyText) {
        return ignore -> new EditableTextTableCell<>(emptyText);
    }

}
