package wtf.choco.aftershock.structure.bin;

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
import wtf.choco.aftershock.App;
import wtf.choco.aftershock.structure.ReplayBin;
import wtf.choco.aftershock.structure.ReplayEntry;

import java.util.ResourceBundle;

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

        ResourceBundle resources = App.getInstance().getResources();

        // Context menu
        this.contextMenu = new ContextMenu();
        MenuItem loadAllReplays = new MenuItem(resources.getString("ui.bin_editor.bin.context_menu.load_all"));
        loadAllReplays.setOnAction(_ -> App.getInstance().getBinRegistry().getGlobalBin().forEach(replay -> replay.setLoaded(bin.hasReplay(replay))));
        MenuItem unloadAllReplays = new MenuItem(resources.getString("ui.bin_editor.bin.context_menu.unload_all"));
        unloadAllReplays.setOnAction(_ -> bin.forEach(replay -> replay.setLoaded(false)));

        MenuItem cloneBin = new MenuItem(resources.getString("ui.bin_editor.bin.context_menu.clone"));
        cloneBin.setOnAction(_ -> App.getInstance().getBinRegistry().addBin(new ReplayBin(bin)));

        this.contextMenu.getItems().addAll(loadAllReplays, unloadAllReplays, new SeparatorMenuItem(), cloneBin);

        // Menu items that should not be accessible to the global bin
        if (!bin.isGlobalBin()) {
            MenuItem renameBin = new MenuItem(resources.getString("ui.bin_editor.bin.context_menu.rename"));
            renameBin.setOnAction(_ -> openNameEditor());
            MenuItem clearBin = new MenuItem(resources.getString("ui.bin_editor.bin.context_menu.clear"));
            clearBin.setOnAction(_ -> bin.clear());
            MenuItem deleteBin = new MenuItem(resources.getString("ui.bin_editor.bin.context_menu.delete"));
            deleteBin.setOnAction(_ -> App.getInstance().getController().getBinEditor().deleteBin(bin, !App.getInstance().getKeybindRegistry().isDown(KeyCode.CONTROL)));

            MenuItem hideBin = new MenuItem(resources.getString("ui.bin_editor.bin.context_menu.hide"));
            hideBin.setOnAction(_ -> App.getInstance().getController().getBinEditor().hideBin(bin));

            this.contextMenu.getItems().addAll(renameBin, clearBin, deleteBin, new SeparatorMenuItem(), hideBin);
        }

        // Layout and listeners
        this.label.setTextAlignment(TextAlignment.CENTER);
        this.label.setWrapText(true);

        this.nameEditor.setAlignment(Pos.TOP_CENTER);

        this.setOnContextMenuRequested(event -> {
            this.contextMenu.show(this, event.getScreenX(), event.getScreenY());
            event.consume();
        });
        this.setOnMouseEntered(_ -> setCursor(Cursor.HAND));
        this.setOnMouseExited(_ -> setCursor(Cursor.DEFAULT));
        this.setOnMouseClicked(event -> {
            MouseButton button = event.getButton();
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

            if (event.isControlDown()) {
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

                if (event.isShiftDown()) {
                    ObservableList<Integer> selectedIndices = selection.getSelectedIndices();
                    if (!selectedIndices.isEmpty()) {
                        int mostRecentIndex = selectedIndices.getLast();
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

        this.setOnDragOver(event -> {
            if (!bin.isGlobalBin() && event.getGestureSource() == App.getInstance().getController().getReplayTable()) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
        });

        this.setOnDragDropped(event -> {
            Dragboard dragboard = event.getDragboard();
            boolean success = false;

            if (dragboard.hasString()) {
                String[] replayIds = dragboard.getString().split(";");
                for (String replayId : replayIds) {
                    ReplayEntry replay = App.getInstance().getBinRegistry().getGlobalBin().getReplayById(replayId);
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
            event.setDropCompleted(success);
        });

        this.label.setOnMouseEntered(_ -> selectedOnly(() -> label.setCursor(Cursor.TEXT)));
        this.label.setOnMouseExited(_ -> selectedOnly(() -> label.setCursor(getCursor())));
        this.label.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                this.selectedOnly(this::openNameEditor);
            }
        });

        this.nameEditor.setOnKeyPressed(event -> {
            KeyCode key = event.getCode();

            if (key == KeyCode.ESCAPE) {
                this.closeNameEditor(false);
            } else if (key == KeyCode.ENTER) {
                this.closeNameEditor(true);
            }
        });

        ImageView graphic = new ImageView(icon);
        this.bin.getReplaysObservable().addListener((ListChangeListener<ReplayEntry>) change -> {
            if (change.getList().isEmpty()) {
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

    public void openNameEditor() {
        ObservableList<Node> children = getChildren();
        if (!children.contains(label)) {
            return;
        }

        this.nameEditor.setText(label.getText());
        this.nameEditor.selectAll();
        children.remove(label);
        children.add(nameEditor);

        this.nameEditor.requestFocus();
        this.beingEdited = true;
    }

    public void closeNameEditor(boolean updateName) {
        ObservableList<Node> children = getChildren();
        if (!children.contains(nameEditor)) {
            return;
        }

        if (updateName) {
            this.bin.setName(nameEditor.getText());
        }

        children.remove(nameEditor);
        children.add(label);

        this.beingEdited = false;
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
