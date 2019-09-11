package wtf.choco.aftershock.structure.bin;

import wtf.choco.aftershock.App;
import wtf.choco.aftershock.manager.BinRegistry;
import wtf.choco.aftershock.replay.Replay;
import wtf.choco.aftershock.structure.ReplayBin;

import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

public class BinDisplayComponent extends VBox {

    private final ReplayBin bin;

    private final ImageView graphic;
    private final Label label;
    private final TextField nameEditor;
    private final ContextMenu contextMenu;

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

        this.contextMenu = new ContextMenu();
        MenuItem loadAllReplays = new MenuItem("Load all replays");
        loadAllReplays.setOnAction(e -> BinRegistry.GLOBAL_BIN.forEach(r -> r.getEntryData().setLoaded(bin.hasReplay(r))));
        MenuItem unloadAllReplays = new MenuItem("Unload all replays");
        unloadAllReplays.setOnAction(e -> bin.forEach(r -> r.getEntryData().setLoaded(false)));
        this.contextMenu.getItems().addAll(loadAllReplays, unloadAllReplays);

        if (!bin.isGlobalBin()) {
            MenuItem clearReplays = new MenuItem("Clear bin");
            clearReplays.setOnAction(e -> bin.clear());
            this.contextMenu.getItems().addAll(new SeparatorMenuItem(), clearReplays);
        }

        this.label.setTextAlignment(TextAlignment.CENTER);
        this.label.setWrapText(true);

        this.nameEditor.setAlignment(Pos.TOP_CENTER);

        this.setOnContextMenuRequested(e -> contextMenu.show(this, e.getScreenX(), e.getScreenY()));
        this.setOnMouseEntered(e -> setCursor(Cursor.HAND));
        this.setOnMouseExited(e -> setCursor(Cursor.DEFAULT));
        this.setOnMouseClicked(e -> {
            if (e.getButton() != MouseButton.PRIMARY) {
                return;
            }

            BinEditor binEditor = App.getInstance().getController().getBinEditor();
            BinSelectionModel selection = binEditor.getSelectionModel();

            if (e.isControlDown()) {
                if (!selection.isSelected(bin)) {
                    selection.select(bin);
                    if (binEditor.getDisplayed() == null) {
                        binEditor.display(bin);
                    }
                } else {
                    selection.clearSelection(bin);
                }
            } else {
                if (beingEdited) {
                    return;
                }

                boolean wasSelected = selection.isSelected(bin) && selection.getSelectedItems().size() > 1;
                selection.clearSelection();
                binEditor.display(wasSelected || binEditor.getDisplayed() != bin ? bin : null);
            }
        });

        this.setOnDragOver(e -> {
            if (!bin.isGlobalBin() && e.getGestureSource() == App.getInstance().getController().getReplayTable()) {
                e.acceptTransferModes(TransferMode.MOVE);
            }
        });

        this.setOnDragDropped(e -> {
            Dragboard dragboard = e.getDragboard();
            boolean success = false;

            if (dragboard.hasString()) {
                String[] replayIds = dragboard.getString().split(";");
                for (String replayId : replayIds) {
                    Replay replay = BinRegistry.GLOBAL_BIN.getReplayById(replayId);
                    if (replay == null || bin.hasReplay(replay)) {
                        continue;
                    }

                    this.bin.addReplay(replay);
                }

                success = true;
            }

            BinEditor binEditor = App.getInstance().getController().getBinEditor();
            binEditor.getSelectionModel().clearSelection();
            binEditor.display(bin);
            e.setDropCompleted(success);
        });

        this.label.setOnMouseEntered(e -> selectedOnly(() -> label.setCursor(Cursor.TEXT)));
        this.label.setOnMouseExited(e -> selectedOnly(() -> label.setCursor(getCursor())));
        this.label.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                this.selectedOnly(this::openNameEditor);
            }
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

        this.nameEditor.requestFocus();

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

    private void selectedOnly(Runnable runnable) {
        if (bin.isGlobalBin() || !App.getInstance().getController().getBinEditor().getSelectionModel().isSelected(bin)) {
            return;
        }

        runnable.run();
    }

}
