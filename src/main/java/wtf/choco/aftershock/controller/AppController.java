package wtf.choco.aftershock.controller;

import java.io.IOException;
import java.util.ResourceBundle;

import wtf.choco.aftershock.App;
import wtf.choco.aftershock.replay.Team;
import wtf.choco.aftershock.structure.ReplayEntry;
import wtf.choco.aftershock.structure.ReplayPropertyFetcher;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;

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

    @FXML private ResourceBundle resources;

    @FXML
    public void initialize() {
        // NOTE: Temporary while working on info panel
        try {
            Parent panel = FXMLLoader.load(App.class.getResource("/InfoPanel.fxml"), ResourceBundle.getBundle("/lang/"));
            this.splitPane.getItems().add(panel);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // END NOTE

        this.columnLoaded.setCellFactory(CheckBoxTableCell.forTableColumn(columnLoaded));
        this.columnLoaded.setCellValueFactory(new ReplayPropertyFetcher<>(ReplayEntry::isLoaded));
        this.columnReplayName.setCellValueFactory(new ReplayPropertyFetcher<>(r -> r.getReplay().getName()));
        this.columnLastModified.setCellValueFactory(new ReplayPropertyFetcher<>(r -> r.getReplay().getDate().toString().replace('T', ' ')));
        this.columnMode.setCellValueFactory(new ReplayPropertyFetcher<>(r -> {
            int size = r.getReplay().getTeamSize();
            return size + "v" + size;
        }));
        this.columnScoreBlue.setCellValueFactory(new ReplayPropertyFetcher<>(r -> r.getReplay().getScore(Team.BLUE)));
        this.columnScoreOrange.setCellValueFactory(new ReplayPropertyFetcher<>(r -> r.getReplay().getScore(Team.ORANGE)));
        this.columnOwner.setCellValueFactory(new ReplayPropertyFetcher<>(r -> r.getReplay().getPlayerName()));
        this.columnComments.setCellValueFactory(new ReplayPropertyFetcher<>(r -> r.getComments().orElse("None")));
    }

    @FXML
    public void openLink(ActionEvent event) {
        Hyperlink hyperlink = (Hyperlink) event.getTarget();
        App.getInstance().getHostServices().showDocument(hyperlink.getTooltip().getText());
    }

    public TableView<ReplayEntry> getReplayTable() {
        return replayTable;
    }

    public void requestLabelUpdate() {
        Platform.runLater(() -> {
            this.labelListed.setText(String.format(resources.getString("ui.footer.listed"), replayTable.getItems().size()));
            this.labelLoaded.setText(String.format(resources.getString("ui.footer.loaded"), replayTable.getItems().stream().filter(ReplayEntry::isLoaded).count()));
            this.labelSelected.setText(String.format(resources.getString("ui.footer.selected"), replayTable.getSelectionModel().getSelectedCells().size()));
        });
    }

}
