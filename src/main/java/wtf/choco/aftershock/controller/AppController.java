package wtf.choco.aftershock.controller;

import java.util.List;
import java.util.ResourceBundle;

import wtf.choco.aftershock.App;
import wtf.choco.aftershock.manager.BinRegistry;
import wtf.choco.aftershock.replay.Team;
import wtf.choco.aftershock.structure.ReplayBin;
import wtf.choco.aftershock.structure.ReplayEntry;
import wtf.choco.aftershock.structure.ReplayPropertyFetcher;
import wtf.choco.aftershock.structure.StringListTableCell;

import javafx.application.Platform;
import javafx.collections.FXCollections;
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
    @FXML private TableColumn<ReplayEntry, String> columnTags; // TODO

    @FXML private SplitPane splitPane;

    @FXML private Label labelListed, labelLoaded, labelSelected;
    @FXML private ProgressBar loadProgress;

    @FXML private ResourceBundle resources;

    private ReplayBin displayedBin = null;

    private double lastKnownDividerPosition = 0.70;
    private int expectedReplays = 1, loadedReplays = 0;

    private ListChangeListener<ReplayEntry> binChangeListener;

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
        this.columnComments.setCellFactory(StringListTableCell.getFactoryCallback());
        this.columnComments.setCellValueFactory(new PropertyValueFactory<>("comments"));

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
                this.splitPane.setDividerPosition(0, lastKnownDividerPosition);
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

        this.binChangeListener = c -> {
            this.setLabel(labelListed, "ui.footer.listed", replayTable.getItems().size());
        };

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
    @SuppressWarnings("unused")
    public void openSettings(ActionEvent event) {
        App.getInstance().openSettingsStage();
    }

    public TableView<ReplayEntry> getReplayTable() {
        return replayTable;
    }

    public void displayBin(ReplayBin bin) {
        this.displayedBin = bin;

        if (bin == null) {
            this.replayTable.setItems(FXCollections.emptyObservableList());
            return;
        }

        ObservableList<ReplayEntry> entries = bin.getObservableList();
        this.replayTable.setItems(entries);

        // Ensure there is only ever one instance of the listener by removing it first
        entries.removeListener(binChangeListener);
        entries.addListener(binChangeListener);
    }

    public ReplayBin getDisplayedBin() {
        return displayedBin;
    }

    public void closeInfoPanel() {
        ObservableList<Node> items = splitPane.getItems();
        if (items.size() > 1) {
            this.lastKnownDividerPosition = splitPane.getDividerPositions()[0];
            items.remove(1, items.size());
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
        for (ReplayEntry replay : replayTable.getItems()) {
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
