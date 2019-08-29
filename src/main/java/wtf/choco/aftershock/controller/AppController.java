package wtf.choco.aftershock.controller;

import java.util.ResourceBundle;

import wtf.choco.aftershock.App;
import wtf.choco.aftershock.replay.Team;
import wtf.choco.aftershock.structure.ReplayEntry;
import wtf.choco.aftershock.structure.ReplayPropertyFetcher;

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
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
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
    @FXML private TableColumn<ReplayEntry, String> columnComments;
    @FXML private TableColumn<ReplayEntry, String> columnTags; // TODO

    @FXML private SplitPane splitPane;

    @FXML private Label labelListed, labelLoaded, labelSelected;
    @FXML private ProgressBar loadProgress;

    @FXML private ResourceBundle resources;

    private double lastKnownDividerPosition = 0.70;
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
        this.columnComments.setCellValueFactory(new ReplayPropertyFetcher<>(r -> r.getComments().orElse("None")));

        this.replayTable.getSelectionModel().getSelectedItems().addListener((ListChangeListener<ReplayEntry>) r -> {
            if (!r.next() || r.getAddedSize() != 1) {
                return;
            }

            this.openInfoPanel(r.getAddedSubList().get(0));
            this.splitPane.setDividerPosition(0, lastKnownDividerPosition);
            this.requestLabelUpdate();
        });

        this.splitPane.setCursor(Cursor.WAIT);
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

    public void setExpectedReplays(int expectedReplays) {
        this.expectedReplays = expectedReplays;
    }

    public void increaseLoadedReplay(int amount) {
        this.loadedReplays += amount;
        this.loadProgress.setProgress(Math.min(((double) loadedReplays) / ((double) expectedReplays), 1.0));

        if (loadProgress.getProgress() >= 1.0) {
            this.loadProgress.setVisible(false);
            this.splitPane.setCursor(Cursor.DEFAULT);
        }
    }

    public void requestLabelUpdate() {
        if (Platform.isFxApplicationThread()) {
            this.labelListed.setText(String.format(resources.getString("ui.footer.listed"), replayTable.getItems().size()));
            this.labelLoaded.setText(String.format(resources.getString("ui.footer.loaded"), replayTable.getItems().stream().filter(ReplayEntry::isLoaded).count()));
            this.labelSelected.setText(String.format(resources.getString("ui.footer.selected"), replayTable.getSelectionModel().getSelectedCells().size()));
        } else {
            Platform.runLater(() -> {
                this.labelListed.setText(String.format(resources.getString("ui.footer.listed"), replayTable.getItems().size()));
                this.labelLoaded.setText(String.format(resources.getString("ui.footer.loaded"), replayTable.getItems().stream().filter(ReplayEntry::isLoaded).count()));
                this.labelSelected.setText(String.format(resources.getString("ui.footer.selected"), replayTable.getSelectionModel().getSelectedCells().size()));
            });
        }
    }

}
