package wtf.choco.aftershock.controller;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import wtf.choco.aftershock.replay.GoalData;
import wtf.choco.aftershock.replay.PlayerData;
import wtf.choco.aftershock.replay.Replay;
import wtf.choco.aftershock.replay.Team;
import wtf.choco.aftershock.util.FXUtils;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.util.Pair;

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
        int teamSize = replay.getTeamSize();
        this.blueGrid.getChildren().removeIf(node -> GridPane.getRowIndex(node) > teamSize);
        this.blueGrid.getRowConstraints().remove(teamSize + 1, blueGrid.getRowCount());
        this.orangeGrid.getChildren().removeIf(node -> GridPane.getRowIndex(node) > teamSize);
        this.orangeGrid.getRowConstraints().remove(teamSize + 1, orangeGrid.getRowCount());

        this.replayName.setText(replay.getName());
        this.replayId.setText(replay.getId());

        List<PlayerData> bluePlayers = new ArrayList<>(teamSize), orangePlayers = new ArrayList<>(teamSize);
        for (PlayerData player : replay.getPlayers()) {
            if (player.getTeam() == Team.BLUE) {
                bluePlayers.add(player);
            } else {
                orangePlayers.add(player);
            }
        }

        this.blueHeader.setText(String.format(resources.getString("ui.replay.stats.team.blue"), replay.getScore(Team.BLUE)));
        this.orangeHeader.setText(String.format(resources.getString("ui.replay.stats.team.orange"), replay.getScore(Team.ORANGE)));

        this.setPlayer(bluePlayers, 0, bluePlayerOne, bluePlayerOneScore, bluePlayerOneGoals, bluePlayerOneAssists, bluePlayerOneSaves, bluePlayerOneShots);
        this.setPlayer(bluePlayers, 1, bluePlayerTwo, bluePlayerTwoScore, bluePlayerTwoGoals, bluePlayerTwoAssists, bluePlayerTwoSaves, bluePlayerTwoShots);
        this.setPlayer(bluePlayers, 2, bluePlayerThree, bluePlayerThreeScore, bluePlayerThreeGoals, bluePlayerThreeAssists, bluePlayerThreeSaves, bluePlayerThreeShots);

        this.setPlayer(orangePlayers, 0, orangePlayerOne, orangePlayerOneScore, orangePlayerOneGoals, orangePlayerOneAssists, orangePlayerOneSaves, orangePlayerOneShots);
        this.setPlayer(orangePlayers, 1, orangePlayerTwo, orangePlayerTwoScore, orangePlayerTwoGoals, orangePlayerTwoAssists, orangePlayerTwoSaves, orangePlayerTwoShots);
        this.setPlayer(orangePlayers, 2, orangePlayerThree, orangePlayerThreeScore, orangePlayerThreeGoals, orangePlayerThreeAssists, orangePlayerThreeSaves, orangePlayerThreeShots);

        int index = 1;
        for (GoalData goal : replay.getGoals()) {
            int time = goal.getSecondsIn();
            Label timeLabel = new Label((time / 60) + ":" + TIME_FORMATTER.format((time % 60)));
            Label playerLabel = new Label(goal.getPlayer() != null ? goal.getPlayer().getName() : "%unknown_player%");

            this.goalGrid.add(timeLabel, 0, index);
            this.goalGrid.add(playerLabel, 1, index++);
        }
    }

    private void setPlayer(List<PlayerData> players, int playerIndex, Label name, Label score, Label goals, Label assists, Label saves, Label shots) {
        if (playerIndex >= players.size()) {
            return; // Just ignore it
        }

        PlayerData player = players.get(playerIndex);

        name.setText(player.getName());
        score.setText(String.valueOf(player.getScore()));
        goals.setText(String.valueOf(player.getGoals()));
        assists.setText(String.valueOf(player.getAssists()));
        saves.setText(String.valueOf(player.getSaves()));
        shots.setText(String.valueOf(player.getShots()));
    }

    public static Parent createInfoPanelFor(Replay replay, ResourceBundle resources) {
        Pair<Parent, InfoPanelController> root = FXUtils.loadFXML("/layout/InfoPanel", resources);

        if (root.getKey() == null) {
            return null;
        }

        root.getValue().loadReplay(replay);
        return root.getKey();
    }

}
