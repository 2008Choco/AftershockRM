package wtf.choco.aftershock.control;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import wtf.choco.aftershock.App;
import wtf.choco.aftershock.event.ReplayBinDisplayEvent;
import wtf.choco.aftershock.structure.ReplayBin;
import wtf.choco.aftershock.structure.ReplayEntry;
import wtf.choco.aftershock.util.FXUtils;
import wtf.choco.aftershock.util.SimpleEventProperty;

public final class ReplayBinDisplay extends VBox {

    @FXML private ImageView icon;
    @FXML private Label nameLabel;
    @FXML private TextField nameTextField;

    private final ObjectProperty<ContextMenu> contextMenu = new SimpleObjectProperty<>(this, "contextMenu");

    private final StringProperty name = new SimpleStringProperty(this, "name", "Unnamed Bin");
    private final BooleanProperty mutable = new SimpleBooleanProperty(this, "mutable", true);
    private final BooleanProperty editingName = new SimpleBooleanProperty(this, "editingName", false);
    private final BooleanProperty active = new SimpleBooleanProperty(this, "active", false);
    private final ListProperty<ReplayEntry> replays = new SimpleListProperty<>(this, "replays", FXCollections.observableArrayList());

    private final SimpleEventProperty<ReplayBinDisplayEvent> onClone = new SimpleEventProperty<>(this::setEventHandler, ReplayBinDisplayEvent.CLONE, this, "onClone");
    private final SimpleEventProperty<ReplayBinDisplayEvent> onDelete = new SimpleEventProperty<>(this::setEventHandler, ReplayBinDisplayEvent.DELETE, this, "onDelete");
    private final SimpleEventProperty<ReplayBinDisplayEvent> onHide = new SimpleEventProperty<>(this::setEventHandler, ReplayBinDisplayEvent.HIDE, this, "onHide");

    public ReplayBinDisplay() {
        FXUtils.loadFXMLComponent("/component/ReplayBinDisplay", this, App.getInstance().getResources());
    }

    //<editor-fold desc="Object property methods">
    public void setContextMenu(ContextMenu contextMenu) {
        this.contextMenuProperty().set(contextMenu);
    }

    public ContextMenu getContextMenu() {
        return contextMenuProperty().get();
    }

    public ObjectProperty<ContextMenu> contextMenuProperty() {
        return contextMenu;
    }

    public void setName(String name) {
        this.nameProperty().set(name);
    }

    public String getName() {
        return nameProperty().get();
    }

    public StringProperty nameProperty() {
        return name;
    }

    public void setMutable(boolean mutable) {
        this.mutableProperty().set(mutable);
    }

    public boolean isMutable() {
        return mutableProperty().get();
    }

    public BooleanProperty mutableProperty() {
        return mutable;
    }

    public void setEditingName(boolean editingName) {
        this.editingNameProperty().set(editingName);
    }

    public boolean isEditingName() {
        return editingNameProperty().get();
    }

    public BooleanProperty editingNameProperty() {
        return editingName;
    }

    public void setActive(boolean active) {
        this.activeProperty().set(active);
    }

    public boolean isActive() {
        return activeProperty().get();
    }

    public BooleanProperty activeProperty() {
        return active;
    }

    public ObservableList<ReplayEntry> getReplays() {
        return replaysProperty().get();
    }

    public ListProperty<ReplayEntry> replaysProperty() {
        return replays;
    }

    public void setOnClone(EventHandler<ReplayBinDisplayEvent> handler) {
        this.onCloneProperty().set(handler);
    }

    public EventHandler<ReplayBinDisplayEvent> getOnClone() {
        return onCloneProperty().get();
    }

    public SimpleEventProperty<ReplayBinDisplayEvent> onCloneProperty() {
        return onClone;
    }

    public void setOnDelete(EventHandler<ReplayBinDisplayEvent> handler) {
        this.onDeleteProperty().set(handler);
    }

    public EventHandler<ReplayBinDisplayEvent> getOnDelete() {
        return onDeleteProperty().get();
    }

    public SimpleEventProperty<ReplayBinDisplayEvent> onDeleteProperty() {
        return onDelete;
    }

    public void setOnHide(EventHandler<ReplayBinDisplayEvent> handler) {
        this.onHideProperty().set(handler);
    }

    public EventHandler<ReplayBinDisplayEvent> getOnHide() {
        return onHideProperty().get();
    }

    public SimpleEventProperty<ReplayBinDisplayEvent> onHideProperty() {
        return onHide;
    }
    //</editor-fold>

    @FXML
    private void initialize() {
        this.setOnContextMenuRequested(event -> {
            ContextMenu contextMenu = getContextMenu();
            if (contextMenu != null) {
                contextMenu.show(ReplayBinDisplay.this, event.getScreenX(), event.getScreenY());
                event.consume();
            }
        });

        // Only one should be visible at a time
        this.nameLabel.textProperty().bindBidirectional(nameProperty());
        this.nameLabel.visibleProperty().bind(editingNameProperty().not());
        this.nameLabel.cursorProperty().bind(Bindings.when(activeProperty().and(mutableProperty())).then(Cursor.TEXT).otherwise(Cursor.DEFAULT));
        this.nameTextField.visibleProperty().bind(editingNameProperty());

        EventHandler<MouseEvent> clickAwayEventHandler = _ -> onClickAwayFromNameTextField();
        this.sceneProperty().addListener((_, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventFilter(MouseEvent.MOUSE_CLICKED, clickAwayEventHandler);
            }

            if (oldScene != null) {
                oldScene.removeEventFilter(MouseEvent.MOUSE_CLICKED, clickAwayEventHandler);
            }
        });

        this.icon.imageProperty().bind(Bindings.when(replaysProperty().emptyProperty()).then(ReplayBin.BIN_GRAPHIC_EMPTY).otherwise(ReplayBin.BIN_GRAPHIC_FULL));

        this.setOnDragOver(this::onDragOver);
        this.setOnDragDropped(this::onDragDropped);
    }

    public void openNameTextField() {
        this.nameTextField.setText(nameLabel.getText());
        this.nameTextField.selectAll();
        this.setEditingName(true);
        this.nameTextField.requestFocus();
    }

    public void closeNameTextField(boolean updateName) {
        String newName = nameTextField.getText().strip();

        if (updateName) {
            this.setName(newName);
        }

        this.setEditingName(false);
    }

    private void onClickAwayFromNameTextField() {
        if (isEditingName() && !nameTextField.isHover()) {
            this.closeNameTextField(true);
        }
    }

    @FXML
    private void onNameLabelMouseClicked(MouseEvent event) {
        // They might be selecting other components, so disallow editing the name while doing this
        if (event.isShiftDown() || event.isControlDown()) {
            return;
        }

        if (!isEditingName() && isActive() && isMutable()) {
            this.openNameTextField();
        }
    }

    @FXML
    private void onNameTextFieldKeyPressed(KeyEvent event) {
        KeyCode key = event.getCode();

        if (key == KeyCode.ESCAPE) {
            this.closeNameTextField(false);
        } else if (key == KeyCode.ENTER) {
            this.closeNameTextField(true);
        }
    }

    @FXML
    private void onLoadAllReplays(ActionEvent event) {
        this.getReplays().forEach(replay -> replay.setLoaded(true));
    }

    @FXML
    private void onUnloadAllReplays(ActionEvent event) {
        this.getReplays().forEach(replay -> replay.setLoaded(false));
    }

    @FXML
    private void onCloneBin(ActionEvent event) {
        this.fireEvent(new ReplayBinDisplayEvent(event.getSource(), this, ReplayBinDisplayEvent.CLONE));
    }

    @FXML
    private void onRenameBin(ActionEvent event) {
        this.openNameTextField();
    }

    @FXML
    private void onClearBin(ActionEvent event) {
        this.getReplays().clear();
    }

    @FXML
    private void onDeleteBin(ActionEvent event) {
        boolean alert = !App.getInstance().getKeybindRegistry().isDown(KeyCode.CONTROL);
        this.fireEvent(new ReplayBinDisplayEvent(event.getSource(), this, ReplayBinDisplayEvent.DELETE, alert));
    }

    @FXML
    private void onHideBin(ActionEvent event) {
        this.fireEvent(new ReplayBinDisplayEvent(event.getSource(), this, ReplayBinDisplayEvent.HIDE));
    }

    private void onDragOver(DragEvent event) {
        if (!isMutable() || event.getGestureSource() != App.getInstance().getController().getReplayTable()) {
            return;
        }

        event.acceptTransferModes(TransferMode.MOVE);
    }

    private void onDragDropped(DragEvent event) {
        Dragboard dragboard = event.getDragboard();
        boolean success = false;

        if (dragboard.hasString()) {
            String[] replayIds = dragboard.getString().split(";");
            for (String replayId : replayIds) {
                ReplayEntry replay = App.getInstance().getBinRegistry().getGlobalBin().getReplay(replayId);
                if (replay == null || getReplays().contains(replay)) {
                    continue;
                }

                this.getReplays().add(replay);
            }

            success = true;
        }

        event.setDropCompleted(success);
    }

}
