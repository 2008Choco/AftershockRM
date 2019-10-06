package wtf.choco.aftershock.structure.bin;

import wtf.choco.aftershock.App;
import wtf.choco.aftershock.manager.BinRegistry;
import wtf.choco.aftershock.replay.Replay;
import wtf.choco.aftershock.structure.ReplayBin;
import wtf.choco.aftershock.structure.ReplayEntry;

import javafx.collections.ListChangeListener;
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

    public BinDisplayComponent(ReplayBin bin, Image icon) {
        this.bin = bin;
        this.label = new Label(bin.getName());
        this.nameEditor = new TextField();

        // Context menu
        this.contextMenu = new ContextMenu();
        MenuItem loadAllReplays = new MenuItem("Load all replays");
        loadAllReplays.setOnAction(e -> BinRegistry.GLOBAL_BIN.forEach(r -> r.getEntryData().setLoaded(bin.hasReplay(r))));
        MenuItem unloadAllReplays = new MenuItem("Unload all replays");
        unloadAllReplays.setOnAction(e -> bin.forEach(r -> r.getEntryData().setLoaded(false)));

        MenuItem cloneBin = new MenuItem("Clone");
        cloneBin.setOnAction(e -> App.getInstance().getBinRegistry().addBin(new ReplayBin(bin)));

        this.contextMenu.getItems().addAll(loadAllReplays, unloadAllReplays, new SeparatorMenuItem(), cloneBin);

        // Menu items that should not be accessible to the global bin
        if (!bin.isGlobalBin()) {
            MenuItem renameBin = new MenuItem("Rename");
            renameBin.setOnAction(e -> openNameEditor());
            MenuItem clearBin = new MenuItem("Clear");
            clearBin.setOnAction(e -> bin.clear());
            MenuItem deleteBin = new MenuItem("Delete");
            deleteBin.setOnAction(e -> App.getInstance().getController().getBinEditor().deleteBin(bin, true, false));

            MenuItem hideBin = new MenuItem("Hide");
            hideBin.setOnAction(e -> App.getInstance().getController().getBinEditor().hideBin(bin));

            this.contextMenu.getItems().addAll(renameBin, clearBin, deleteBin, new SeparatorMenuItem(), hideBin);
        }

        // Layout and listeners
        this.label.setTextAlignment(TextAlignment.CENTER);
        this.label.setWrapText(true);

        this.nameEditor.setAlignment(Pos.TOP_CENTER);

        this.setOnContextMenuRequested(e -> {
            this.contextMenu.show(this, e.getScreenX(), e.getScreenY());
            e.consume();
        });
        this.setOnMouseEntered(e -> setCursor(Cursor.HAND));
        this.setOnMouseExited(e -> setCursor(Cursor.DEFAULT));
        this.setOnMouseClicked(e -> {
            MouseButton button = e.getButton();
            BinEditor binEditor = App.getInstance().getController().getBinEditor();
            BinSelectionModel selection = binEditor.getSelectionModel();

            if (button != MouseButton.PRIMARY) {
                if (button == MouseButton.SECONDARY && selection.getSelectedItems().size() > 1) {
                    selection.clearSelection();
                    binEditor.display(bin);
                }

                return;
            }

            if (beingEdited) {
                return;
            }

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
                boolean wasSelected = selection.isSelected(bin) && selection.getSelectedItems().size() > 1;

                if (e.isShiftDown()) {
                    ObservableList<Integer> selectedIndices = selection.getSelectedIndices();
                    if (selectedIndices.size() >= 1) {
                        int mostRecentIndex = selectedIndices.get(selectedIndices.size() - 1);
                        selection.clearSelection();
                        selection.selectRange(binEditor.indexOf(bin), mostRecentIndex);
                        selection.select(binEditor.getBin(mostRecentIndex));
                    }
                } else {
                    selection.clearSelection();
                }

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

        ImageView graphic = new ImageView(icon);
        this.bin.getReplaysObservable().addListener((ListChangeListener<ReplayEntry>) c -> {
            if (c.getList().isEmpty()) {
                graphic.setImage(ReplayBin.BIN_GRAPHIC_EMPTY);
            } else {
                graphic.setImage(ReplayBin.BIN_GRAPHIC_FULL);
            }
        });

        this.getChildren().addAll(graphic, label);
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
