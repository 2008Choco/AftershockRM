package wtf.choco.aftershock.controller;

import java.awt.Toolkit;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import wtf.choco.aftershock.App;
import wtf.choco.aftershock.manager.BinRegistry;
import wtf.choco.aftershock.replay.Team;
import wtf.choco.aftershock.structure.DynamicFilter;
import wtf.choco.aftershock.structure.ReplayBin;
import wtf.choco.aftershock.structure.ReplayEntry;
import wtf.choco.aftershock.structure.ReplayPropertyFetcher;
import wtf.choco.aftershock.structure.StringListTableCell;
import wtf.choco.aftershock.structure.Tag;
import wtf.choco.aftershock.structure.bin.BinEditor;
import wtf.choco.aftershock.structure.bin.BinSelectionModel;
import wtf.choco.aftershock.util.FXUtils;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TableView.TableViewSelectionModel;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;

public final class AppController {

    public static final DataFormat DATA_FORMAT_REPLAY_ENTRY = new DataFormat();

    private static final Image DRAG_IMAGE = new Image(App.class.getResourceAsStream("/icons/file.png"));

    @FXML private TableView<ReplayEntry> replayTable;

    @FXML private TableColumn<ReplayEntry, Boolean> columnLoaded;
    @FXML private TableColumn<ReplayEntry, String> columnReplayName;
    @FXML private TableColumn<ReplayEntry, String> columnLastModified;
    @FXML private TableColumn<ReplayEntry, String> columnMode;
    @FXML private TableColumn<ReplayEntry, Integer> columnScoreBlue;
    @FXML private TableColumn<ReplayEntry, Integer> columnScoreOrange;
    @FXML private TableColumn<ReplayEntry, String> columnMap;
    @FXML private TableColumn<ReplayEntry, String> columnOwner;
    @FXML private TableColumn<ReplayEntry, List<String>> columnComments;
    @FXML private TableColumn<ReplayEntry, List<Tag>> columnTags;

    @FXML private SplitPane splitPane;

    @FXML private Label labelListed, labelLoaded, labelSelected;
    @FXML private TextField filterBar;
    @FXML private ImageView filterOptionsImage;
    @FXML private ProgressBar loadProgress;

    @FXML private ResourceBundle resources;

    @FXML private HBox primaryDisplay;
    @FXML private VBox binEditorPane, binEditorList;
    @FXML private ScrollPane binEditorScrollPane;

    private BinEditor binEditor;

    private double lastDividerPositionInfo = 0.70;

    private Popup popup = new Popup();

    private DynamicFilter<ReplayEntry> tableFilter = new DynamicFilter<>((r, t) -> r.getReplay().getName().toLowerCase().startsWith(t.toLowerCase()));

    @FXML
    public void initialize() {
        this.columnLoaded.setCellFactory(CheckBoxTableCell.forTableColumn(columnLoaded));
        this.columnLoaded.setCellValueFactory(new PropertyValueFactory<>("loaded"));
        this.columnReplayName.setCellValueFactory(new ReplayPropertyFetcher<>(r -> r.getReplay().getName()));
        this.columnLastModified.setCellValueFactory(new ReplayPropertyFetcher<>(r -> r.getReplay().getDate().toString().replace('T', ' ')));
        this.columnMode.setCellValueFactory(new ReplayPropertyFetcher<>(r -> String.format("%dv%1$d", r.getReplay().getTeamSize())));
        this.columnScoreBlue.setCellValueFactory(new ReplayPropertyFetcher<>(r -> r.getReplay().getScore(Team.BLUE)));
        this.columnScoreOrange.setCellValueFactory(new ReplayPropertyFetcher<>(r -> r.getReplay().getScore(Team.ORANGE)));
        this.columnMap.setCellValueFactory(new ReplayPropertyFetcher<>(r -> r.getReplay().getMapName()));
        this.columnOwner.setCellValueFactory(new ReplayPropertyFetcher<>(r -> r.getReplay().getPlayerName()));
        this.columnComments.setCellFactory(StringListTableCell.getFactoryCallback("None"));
        this.columnComments.setCellValueFactory(new PropertyValueFactory<>("comments"));
        this.columnTags.setCellFactory(StringListTableCell.getFactoryCallback());
        this.columnTags.setCellValueFactory(new PropertyValueFactory<>("tags"));

        TableViewSelectionModel<ReplayEntry> selectionModel = replayTable.getSelectionModel();
        selectionModel.setSelectionMode(SelectionMode.MULTIPLE);

        // TODO: Do this with CSS
        StackPane root = FXUtils.loadFXMLRoot("/layout/FilterPopup", resources);
        this.popup.setAutoHide(true);
        this.popup.getContent().add(root);
        this.popup.setOnHidden(e -> filterOptionsImage.setOpacity(0.5));
        this.popup.setWidth(root.getPrefWidth());
        this.popup.setHeight(root.getPrefHeight());

        this.filterOptionsImage.setCursor(Cursor.HAND);

        // Label update listeners
        ListChangeListener<ReplayEntry> changeListener = (c) -> setLabel(labelListed, "ui.footer.listed", c.getList().size());
        this.replayTable.getItems().addListener(changeListener);
        this.replayTable.itemsProperty().addListener((ChangeListener<ObservableList<ReplayEntry>>) (c, oldValue, newValue) -> {
            this.setLabel(labelListed, "ui.footer.listed", newValue.size());
            newValue.addListener(changeListener);
        });

        selectionModel.getSelectedItems().addListener((ListChangeListener<ReplayEntry>) c -> {
            this.setLabel(labelSelected, "ui.footer.selected", selectionModel.getSelectedItems().size());

            if (!c.next()) {
                return;
            }

            if (c.getAddedSize() > 0) {
                this.openInfoPanel(c.getAddedSubList().get(0));
                this.splitPane.setDividerPosition(splitPane.getDividers().size() - 1, lastDividerPositionInfo);
            }
        });

        BinRegistry.GLOBAL_BIN.getObservableList().addListener((ListChangeListener<ReplayEntry>) c -> {
            if (!c.next()) {
                return;
            }

            int loaded = 0;
            for (ReplayEntry replay : c.getList()) {
                if (replay.isLoaded()) {
                    loaded++;
                }
            }

            this.setLabel(labelLoaded, "ui.footer.loaded", loaded);
        });

        this.replayTable.setOnMouseClicked(e -> replayTable.requestFocus());
        this.replayTable.setOnDragDetected(e -> {
            var selection = replayTable.getSelectionModel();
            if (selection.isEmpty()) {
                return;
            }

            Dragboard dragboard = replayTable.startDragAndDrop(TransferMode.COPY_OR_MOVE);
            dragboard.setDragView(DRAG_IMAGE);

            ClipboardContent clipboard = new ClipboardContent();
            StringBuilder replays = new StringBuilder();
            List<File> files = new ArrayList<>(selection.getSelectedItems().size());
            for (ReplayEntry replay : selection.getSelectedItems()) {
                replays.append(replay.getReplay().getId());
                replays.append(";");

                files.add(replay.getReplay().getDemoFile());
            }

            clipboard.putFiles(files);
            clipboard.putString(replays.toString().substring(0, replays.length() - 1));
            dragboard.setContent(clipboard);
        });

        this.binEditor = new BinEditor(this, binEditorPane, binEditorList, c -> setLabel(labelListed, "ui.footer.listed", replayTable.getItems().size()));

        ContextMenu binEditorContextMenu = new ContextMenu();
        MenuItem showHiddenBins = new MenuItem("Unhide bins");
        showHiddenBins.setOnAction(e -> new ArrayList<>(binEditor.getHidden()).forEach(binEditor::unhide));
        binEditorContextMenu.getItems().add(showHiddenBins);
        this.binEditorScrollPane.setContextMenu(binEditorContextMenu);

        // TODO: Add functionality for "open file location"
        ContextMenu tableContextMenu = new ContextMenu();
        tableContextMenu.getItems().add(new MenuItem("Open file location..."));
        Menu sendTo = new Menu("Send to...");

        this.replayTable.setContextMenu(tableContextMenu);

        this.replayTable.setOnContextMenuRequested(e -> {
            if (replayTable.getSelectionModel().isEmpty()) {
                tableContextMenu.hide();
                e.consume();
                return;
            }

            BinRegistry binRegistry = App.getInstance().getBinRegistry();

            tableContextMenu.getItems().add(1, sendTo);
            sendTo.getItems().clear();

            for (ReplayBin bin : binRegistry.getBins()) {
                if (bin.isGlobalBin() || bin == binEditor.getDisplayed()) {
                    continue;
                }

                MenuItem item = new MenuItem(bin.getName());
                item.setOnAction(itemEvent -> replayTable.getSelectionModel().getSelectedItems().forEach(i -> bin.addReplay(i.getReplay())));
                sendTo.getItems().add(item);
            }

            if (sendTo.getItems().isEmpty()) {
                tableContextMenu.getItems().remove(1);
            }
        });

        // Zero the labels on init (no placeholder %s should be visible)
        this.setLabel(labelListed, "ui.footer.listed", 0);
        this.setLabel(labelLoaded, "ui.footer.loaded", 0);
        this.setLabel(labelSelected, "ui.footer.selected", 0);
    }

    @FXML
    public void openLink(ActionEvent event) {
        Hyperlink hyperlink = (Hyperlink) event.getTarget();
        App.getInstance().getHostServices().showDocument(hyperlink.getTooltip().getText());
    }

    @FXML
    public void openSettings(@SuppressWarnings("unused") ActionEvent event) {
        App.getInstance().openSettingsStage();
    }

    @FXML
    public void toggleBinEditor(@SuppressWarnings("unused") ActionEvent event) {
        ObservableList<Node> primaryDisplayChildren = primaryDisplay.getChildren();
        if (primaryDisplayChildren.size() == 1) {
            primaryDisplayChildren.add(0, binEditor.getNode());
        } else {
            primaryDisplayChildren.remove(binEditor.getNode());
        }
    }

    @FXML
    public void createBin(@SuppressWarnings("unused") ActionEvent event) {
        int duplicateCount = 0;
        String name = "New Bin";

        BinRegistry binRegistry = App.getInstance().getBinRegistry();
        ReplayBin bin = null;
        while ((bin = binRegistry.createBin(name + (duplicateCount++ >= 1 ? " (" + duplicateCount + ")" : ""))) == null);

        this.binEditor.getSelectionModel().clearSelection();
        this.binEditor.display(bin);
        bin.getDisplay().openNameEditor();
    }

    @FXML
    public void deleteBin(@SuppressWarnings("unused") ActionEvent event) {
        BinRegistry binRegistry = App.getInstance().getBinRegistry();

        BinSelectionModel selection = binEditor.getSelectionModel();
        ArrayList<ReplayBin> toDelete = new ArrayList<>(selection.getSelectedItems());

        toDelete.forEach(b -> {
            if (b.isGlobalBin()) { // Don't delete the global bin
                Toolkit.getDefaultToolkit().beep();
                return;
            }

            if (binEditor.getDisplayed() == b) {
                this.binEditor.clearDisplay();
            }

            selection.clearSelection(b);
            binRegistry.deleteBin(b);
        });

        if (binEditor.getDisplayed() == null) {
            this.binEditor.display(selection.isEmpty() ? BinRegistry.GLOBAL_BIN : selection.getSelectedItems().get(0));
        }
    }

    @FXML
    public void updateFilter(@SuppressWarnings("unused") KeyEvent event) {
        this.getTableFilter().setTerm(filterBar.getText());

        ObservableList<ReplayEntry> items = replayTable.getItems();
        if (items instanceof FilteredList) {
            FilteredList<ReplayEntry> filteredItems = (FilteredList<ReplayEntry>) items;
            filteredItems.setPredicate(null); // Must set to null first to invalidate the predicate... stupid
            filteredItems.setPredicate(getTableFilter());
        }
    }

    @FXML
    public void exitFilter(KeyEvent event) {
        KeyCode pressed = event.getCode();
        if (pressed == KeyCode.ESCAPE || pressed == KeyCode.ENTER) {
            this.replayTable.requestFocus();
            event.consume();
            return;
        }
    }

    @FXML
    public void toggleFilterMenu(MouseEvent event) {
        Object clicked = event.getSource();
        if (clicked instanceof ImageView) {
            ImageView clickedImage = (ImageView) clicked;

            if (!popup.isShowing()) {
                clickedImage.setOpacity(1.0);

                Bounds imageBounds = clickedImage.localToScreen(clickedImage.getBoundsInLocal());
                this.popup.show(clickedImage, imageBounds.getCenterX() - (popup.getWidth() / 2), imageBounds.getCenterY() - 15 - popup.getHeight());
            } else {
                clickedImage.setOpacity(0.5);
                this.popup.hide();
            }
        }
    }

    public TableView<ReplayEntry> getReplayTable() {
        return replayTable;
    }

    public TextField getFilterBar() {
        return filterBar;
    }

    public BinEditor getBinEditor() {
        return binEditor;
    }

    public DynamicFilter<ReplayEntry> getTableFilter() {
        return tableFilter;
    }

    public void closeInfoPanel() {
        ObservableList<Node> items = splitPane.getItems();
        if (items.get(items.size() - 1) != replayTable) {
            this.lastDividerPositionInfo = splitPane.getDividerPositions()[splitPane.getDividers().size() - 1];
            items.remove(items.size() - 1);
        }
    }

    public void openInfoPanel(ReplayEntry replay) {
        this.closeInfoPanel();
        this.splitPane.getItems().add(InfoPanelController.createInfoPanelFor(replay, resources));
    }

    public void startLoading() {
        if (loadProgress.isVisible()) {
            return;
        }

        this.setLoadingProgress(0.0);
        this.loadProgress.setVisible(true);
        App.getInstance().getStage().getScene().setCursor(Cursor.WAIT);
    }

    public void setLoadingProgress(double progress) {
        this.loadProgress.setProgress(progress);
    }

    public void stopLoading() {
        if (!loadProgress.isVisible()) {
            return;
        }

        this.setLoadingProgress(1.0);
        this.loadProgress.setVisible(false);
        App.getInstance().getStage().getScene().setCursor(Cursor.DEFAULT);
    }

    public void updateLoadedLabel() {
        int loaded = 0;
        for (ReplayEntry replay : BinRegistry.GLOBAL_BIN.getReplays()) {
            if (replay.isLoaded()) {
                loaded++;
            }
        }

        this.setLabel(labelLoaded, "ui.footer.loaded", loaded);
    }

    private void setLabel(Label label, String resourceKey, int amount) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> setLabel(label, resourceKey, amount));
            return;
        }

        label.setText(String.format(resources.getString(resourceKey), amount));
    }

}
