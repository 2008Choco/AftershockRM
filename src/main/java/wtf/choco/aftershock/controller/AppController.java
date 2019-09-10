package wtf.choco.aftershock.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import wtf.choco.aftershock.App;
import wtf.choco.aftershock.manager.BinRegistry;
import wtf.choco.aftershock.replay.Team;
import wtf.choco.aftershock.structure.BinEditor;
import wtf.choco.aftershock.structure.BinSelectionModel;
import wtf.choco.aftershock.structure.ReplayBin;
import wtf.choco.aftershock.structure.ReplayEntry;
import wtf.choco.aftershock.structure.ReplayPropertyFetcher;
import wtf.choco.aftershock.structure.StringListTableCell;
import wtf.choco.aftershock.structure.Tag;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TableView.TableViewSelectionModel;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public final class AppController {

    @FXML private TableView<ReplayEntry> replayTable;

    @FXML private TableColumn<ReplayEntry, Boolean> columnLoaded;
    @FXML private TableColumn<ReplayEntry, String> columnReplayName;
    @FXML private TableColumn<ReplayEntry, String> columnLastModified;
    @FXML private TableColumn<ReplayEntry, String> columnMode;
    @FXML private TableColumn<ReplayEntry, Integer> columnScoreBlue;
    @FXML private TableColumn<ReplayEntry, Integer> columnScoreOrange;
    @FXML private TableColumn<ReplayEntry, String> columnOwner;
    @FXML private TableColumn<ReplayEntry, List<String>> columnComments;
    @FXML private TableColumn<ReplayEntry, List<Tag>> columnTags;

    @FXML private SplitPane splitPane;

    @FXML private Label labelListed, labelLoaded, labelSelected;
    @FXML private ProgressBar loadProgress;

    @FXML private ResourceBundle resources;

    @FXML private HBox primaryDisplay;
    @FXML private VBox binEditorPane, binEditorList;

    private BinEditor binEditor;

    private double lastDividerPositionInfo = 0.70;
    private int expectedReplays = 1, loadedReplays = 0;

    @FXML
    public void initialize() {
        this.columnLoaded.setCellFactory(CheckBoxTableCell.forTableColumn(columnLoaded));
        this.columnLoaded.setCellValueFactory(new PropertyValueFactory<>("loaded"));
        this.columnReplayName.setCellValueFactory(new ReplayPropertyFetcher<>(r -> r.getReplay().getName()));
        this.columnLastModified.setCellValueFactory(new ReplayPropertyFetcher<>(r -> r.getReplay().getDate().toString().replace('T', ' ')));
        this.columnMode.setCellValueFactory(new ReplayPropertyFetcher<>(r -> String.format("%dv%1$d", r.getReplay().getTeamSize())));
        this.columnScoreBlue.setCellValueFactory(new ReplayPropertyFetcher<>(r -> r.getReplay().getScore(Team.BLUE)));
        this.columnScoreOrange.setCellValueFactory(new ReplayPropertyFetcher<>(r -> r.getReplay().getScore(Team.ORANGE)));
        this.columnOwner.setCellValueFactory(new ReplayPropertyFetcher<>(r -> r.getReplay().getPlayerName()));
        this.columnComments.setCellFactory(StringListTableCell.getFactoryCallback("None"));
        this.columnComments.setCellValueFactory(new PropertyValueFactory<>("comments"));
        this.columnTags.setCellFactory(StringListTableCell.getFactoryCallback());
        this.columnTags.setCellValueFactory(new PropertyValueFactory<>("tags"));

        TableViewSelectionModel<ReplayEntry> selectionModel = replayTable.getSelectionModel();
        selectionModel.setSelectionMode(SelectionMode.MULTIPLE);

        // Label update listeners
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

        this.binEditor = new BinEditor(replayTable, binEditorPane, binEditorList, c -> setLabel(labelListed, "ui.footer.listed", replayTable.getItems().size()));

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
        ObservableList<ReplayBin> selected = selection.getSelectedItems();
        List<ReplayBin> deleted = new ArrayList<>(selected.size());

        selected.forEach(b -> {
            if (b == BinRegistry.GLOBAL_BIN) { // Don't delete the global bin
                return;
            }

            if (binEditor.getDisplayed() == b) {
                this.binEditor.clearDisplay();
            }

            binRegistry.deleteBin(b);
            deleted.add(b);
        });

        deleted.forEach(selection::clearSelection);

        if (binEditor.getDisplayed() == null) {
            this.binEditor.display(selected.size() > 0 ? selected.get(0) : BinRegistry.GLOBAL_BIN);
        }
    }

    public TableView<ReplayEntry> getReplayTable() {
        return replayTable;
    }

    public BinEditor getBinEditor() {
        return binEditor;
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

    public void prepareLoading(int expectedReplays) {
        this.expectedReplays = expectedReplays;
        this.splitPane.setCursor(Cursor.WAIT);
        this.loadProgress.setVisible(true);
    }

    public void increaseLoadedReplay(int amount) {
        this.loadedReplays += amount;
        this.loadProgress.setProgress(Math.min(((double) loadedReplays) / ((double) expectedReplays), 1.0));

        if (loadProgress.getProgress() >= 1.0) {
            this.splitPane.setCursor(Cursor.DEFAULT);
            this.loadProgress.setVisible(false);
        }
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
