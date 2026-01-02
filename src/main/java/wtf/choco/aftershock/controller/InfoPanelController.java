package wtf.choco.aftershock.controller;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import wtf.choco.aftershock.replay.Goal;
import wtf.choco.aftershock.replay.Replay;
import wtf.choco.aftershock.replay.Player;
import wtf.choco.aftershock.replay.Team;
import wtf.choco.aftershock.util.FXUtils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;

public final class InfoPanelController {

    private static final NumberFormat TIME_FORMATTER = new DecimalFormat("00");

    @FXML private GridPane blueGrid, orangeGrid;

    @FXML private Label replayName, replayId;

    @FXML private Label blueHeader;
    @FXML private Label bluePlayerOne, bluePlayerOneScore, bluePlayerOneGoals, bluePlayerOneAssists, bluePlayerOneSaves, bluePlayerOneShots;
    @FXML private Label bluePlayerTwo, bluePlayerTwoScore, bluePlayerTwoGoals, bluePlayerTwoAssists, bluePlayerTwoSaves, bluePlayerTwoShots;
    @FXML private Label bluePlayerThree, bluePlayerThreeScore, bluePlayerThreeGoals, bluePlayerThreeAssists, bluePlayerThreeSaves, bluePlayerThreeShots;

    @FXML private Label orangeHeader;
    @FXML private Label orangePlayerOne, orangePlayerOneScore, orangePlayerOneGoals, orangePlayerOneAssists, orangePlayerOneSaves, orangePlayerOneShots;
    @FXML private Label orangePlayerTwo, orangePlayerTwoScore, orangePlayerTwoGoals, orangePlayerTwoAssists, orangePlayerTwoSaves, orangePlayerTwoShots;
    @FXML private Label orangePlayerThree, orangePlayerThreeScore, orangePlayerThreeGoals, orangePlayerThreeAssists, orangePlayerThreeSaves, orangePlayerThreeShots;

    @FXML private GridPane goalGrid;

    @FXML private ResourceBundle resources;

    private void loadReplay(Replay replay) {
        int teamSize = replay.teamSize();
        this.blueGrid.getChildren().removeIf(node -> GridPane.getRowIndex(node) > teamSize);
        this.blueGrid.getRowConstraints().remove(teamSize + 1, blueGrid.getRowCount());
        this.orangeGrid.getChildren().removeIf(node -> GridPane.getRowIndex(node) > teamSize);
        this.orangeGrid.getRowConstraints().remove(teamSize + 1, orangeGrid.getRowCount());

        this.replayName.setText(replay.name());
        this.replayId.setText(replay.id());

        this.blueHeader.setText(String.format(resources.getString("ui.replay.stats.team.blue"), replay.score(Team.BLUE)));
        this.orangeHeader.setText(String.format(resources.getString("ui.replay.stats.team.orange"), replay.score(Team.ORANGE)));

        List<Player> bluePlayers = replay.players(Team.BLUE);
        this.setPlayer(bluePlayers, 0, bluePlayerOne, bluePlayerOneScore, bluePlayerOneGoals, bluePlayerOneAssists, bluePlayerOneSaves, bluePlayerOneShots);
        this.setPlayer(bluePlayers, 1, bluePlayerTwo, bluePlayerTwoScore, bluePlayerTwoGoals, bluePlayerTwoAssists, bluePlayerTwoSaves, bluePlayerTwoShots);
        this.setPlayer(bluePlayers, 2, bluePlayerThree, bluePlayerThreeScore, bluePlayerThreeGoals, bluePlayerThreeAssists, bluePlayerThreeSaves, bluePlayerThreeShots);

        List<Player> orangePlayers = replay.players(Team.ORANGE);
        this.setPlayer(orangePlayers, 0, orangePlayerOne, orangePlayerOneScore, orangePlayerOneGoals, orangePlayerOneAssists, orangePlayerOneSaves, orangePlayerOneShots);
        this.setPlayer(orangePlayers, 1, orangePlayerTwo, orangePlayerTwoScore, orangePlayerTwoGoals, orangePlayerTwoAssists, orangePlayerTwoSaves, orangePlayerTwoShots);
        this.setPlayer(orangePlayers, 2, orangePlayerThree, orangePlayerThreeScore, orangePlayerThreeGoals, orangePlayerThreeAssists, orangePlayerThreeSaves, orangePlayerThreeShots);

        int index = 1;
        for (Goal goal : replay.goals()) {
            long time = goal.timestamp(replay, TimeUnit.SECONDS);
            Label timeLabel = new Label((time / 60) + ":" + TIME_FORMATTER.format((time % 60)));
            Label playerLabel = new Label(goal.playerName());

            this.goalGrid.add(timeLabel, 0, index);
            this.goalGrid.add(playerLabel, 1, index++);
        }
    }

    private void setPlayer(List<Player> players, int playerIndex, Label name, Label score, Label goals, Label assists, Label saves, Label shots) {
        if (playerIndex >= players.size()) {
            return; // Just ignore it
        }

        Player player = players.get(playerIndex);

        name.setText(player.name());
        score.setText(String.valueOf(player.score()));
        goals.setText(String.valueOf(player.goals()));
        assists.setText(String.valueOf(player.assists()));
        saves.setText(String.valueOf(player.saves()));
        shots.setText(String.valueOf(player.shots()));
    }

    public static Parent createInfoPanelFor(Replay replay, ResourceBundle resources) {
        var infoPanelFXML = FXUtils.<Parent, InfoPanelController>loadFXML("/layout/InfoPanel", resources);
        infoPanelFXML.controller().loadReplay(replay);
        return infoPanelFXML.root();
    }

}
