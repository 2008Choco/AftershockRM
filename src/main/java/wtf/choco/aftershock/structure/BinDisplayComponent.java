package wtf.choco.aftershock.structure;

import wtf.choco.aftershock.App;
import wtf.choco.aftershock.controller.AppController;

import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

public class BinDisplayComponent extends VBox {

    private final ReplayBin bin;

    private final ImageView graphic;
    private final Label label;
    private final TextField nameEditor;
    private boolean beingEdited = false;

    {
        this.setAlignment(Pos.TOP_CENTER);
        this.setMaxSize(75.0, 65.0);
        this.setSpacing(5.0);
        this.setPadding(new Insets(5.0, 5.0, 5.0, 5.0));

        this.getStyleClass().add("bin-display");
    }

    public BinDisplayComponent(ReplayBin bin, Image graphic) {
        this.bin = bin;
        this.graphic = new ImageView(graphic);
        this.label = new Label(bin.getName());
        this.nameEditor = new TextField();

        this.label.setTextAlignment(TextAlignment.CENTER);
        this.label.setWrapText(true);

        this.nameEditor.setAlignment(Pos.TOP_CENTER);

        this.setOnMouseEntered(e -> setCursor(Cursor.HAND));
        this.setOnMouseExited(e -> setCursor(Cursor.DEFAULT));
        this.setOnMouseClicked(e -> {
            if (e.getButton() != MouseButton.PRIMARY) {
                return;
            }

            AppController controller = App.getInstance().getController();

            if (e.isControlDown()) {
                if (!controller.isSelectedBin(bin)) {
                    controller.selectBin(bin);
                    if (controller.getDisplayedBin() == null) {
                        controller.displayBin(bin);
                    }
                } else {
                    controller.deselectBin(bin);
                }
            } else {
                boolean wasSelected = controller.isSelectedBin(bin) && controller.getSelectedCount() > 1;
                controller.clearSelectedBins();
                controller.displayBin(wasSelected || controller.getDisplayedBin() != bin ? bin : null);
            }
        });

        this.label.setOnMouseClicked(e -> {
            AppController controller = App.getInstance().getController();
            if (!controller.isSelectedBin(bin)) {
                return;
            }

            this.openNameEditor();
        });

        this.nameEditor.setOnKeyPressed(e -> {
            KeyCode key = e.getCode();

            if (key == KeyCode.ESCAPE) {
                this.closeNameEditor(false);
            } else if (key == KeyCode.ENTER) {
                this.closeNameEditor(true);
            }
        });

        this.getChildren().addAll(this.graphic, label);
    }

    public ReplayBin getBin() {
        return bin;
    }

    public void updateName() {
        this.label.setText(bin.getName());
    }

    public boolean openNameEditor() {
        ObservableList<Node> children = getChildren();
        if (!children.contains(label)) {
            return false;
        }

        this.nameEditor.setText(label.getText());
        this.nameEditor.selectAll();
        children.remove(label);
        children.add(nameEditor);

        this.beingEdited = true;
        return true;
    }

    public boolean closeNameEditor(boolean updateName) {
        ObservableList<Node> children = getChildren();
        if (!children.contains(nameEditor)) {
            return false;
        }

        if (updateName) {
            this.bin.setName(nameEditor.getText());
        }

        children.remove(nameEditor);
        children.add(label);

        this.beingEdited = false;
        return true;
    }

    public boolean isBeingEdited() {
        return beingEdited;
    }

}
