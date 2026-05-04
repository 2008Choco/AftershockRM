package wtf.choco.aftershock.controller;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import wtf.choco.aftershock.App;
import wtf.choco.aftershock.replay.Goal;
import wtf.choco.aftershock.replay.Player;
import wtf.choco.aftershock.replay.Replay;
import wtf.choco.aftershock.replay.Team;
import wtf.choco.aftershock.structure.ReplayEntry;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.ToIntFunction;

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

    @FXML
    public void initialize() {
        ObjectProperty<ReplayEntry> replayProperty = App.getInstance().detailedReplayProperty();

        this.replayName.textProperty().bind(replayProperty.map(ReplayEntry::name));
        this.replayId.textProperty().bind(replayProperty.map(ReplayEntry::id));

        this.blueHeader.textProperty().bind(replayProperty.map(entry -> String.format(resources.getString("ui.replay.stats.team.blue"), entry.score(Team.BLUE))));
        this.orangeHeader.textProperty().bind(replayProperty.map(entry -> String.format(resources.getString("ui.replay.stats.team.orange"), entry.score(Team.ORANGE))));

        this.bindPlayerLabels(replayProperty, Team.BLUE, 0, bluePlayerOne, bluePlayerOneScore, bluePlayerOneGoals, bluePlayerOneAssists, bluePlayerOneSaves, bluePlayerOneShots);
        this.bindPlayerLabels(replayProperty, Team.BLUE, 1, bluePlayerTwo, bluePlayerTwoScore, bluePlayerTwoGoals, bluePlayerTwoAssists, bluePlayerTwoSaves, bluePlayerTwoShots);
        this.bindPlayerLabels(replayProperty, Team.BLUE, 2, bluePlayerThree, bluePlayerThreeScore, bluePlayerThreeGoals, bluePlayerThreeAssists, bluePlayerThreeSaves, bluePlayerThreeShots);

        this.bindPlayerLabels(replayProperty, Team.ORANGE, 0, orangePlayerOne, orangePlayerOneScore, orangePlayerOneGoals, orangePlayerOneAssists, orangePlayerOneSaves, orangePlayerOneShots);
        this.bindPlayerLabels(replayProperty, Team.ORANGE, 1, orangePlayerTwo, orangePlayerTwoScore, orangePlayerTwoGoals, orangePlayerTwoAssists, orangePlayerTwoSaves, orangePlayerTwoShots);
        this.bindPlayerLabels(replayProperty, Team.ORANGE, 2, orangePlayerThree, orangePlayerThreeScore, orangePlayerThreeGoals, orangePlayerThreeAssists, orangePlayerThreeSaves, orangePlayerThreeShots);

        // Listen for replay changes and update the goal timeline
        replayProperty.addListener((_, _, newValue) -> updateGoalTimeline(newValue));
    }

    private void bindPlayerLabels(ObjectProperty<ReplayEntry> replayProperty, Team team, int playerIndex, Label name, Label score, Label goals, Label assists, Label saves, Label shots) {
        name.textProperty().bind(Bindings.createStringBinding(() -> getPlayerLabelText(replayProperty, team, playerIndex, Player::name), replayProperty));
        score.textProperty().bind(Bindings.createStringBinding(() -> getPlayerLabelText(replayProperty, team, playerIndex, Player::score), replayProperty));
        goals.textProperty().bind(Bindings.createStringBinding(() -> getPlayerLabelText(replayProperty, team, playerIndex, Player::goals), replayProperty));
        assists.textProperty().bind(Bindings.createStringBinding(() -> getPlayerLabelText(replayProperty, team, playerIndex, Player::assists), replayProperty));
        saves.textProperty().bind(Bindings.createStringBinding(() -> getPlayerLabelText(replayProperty, team, playerIndex, Player::saves), replayProperty));
        shots.textProperty().bind(Bindings.createStringBinding(() -> getPlayerLabelText(replayProperty, team, playerIndex, Player::shots), replayProperty));
    }

    private String getPlayerLabelText(ObjectProperty<ReplayEntry> replayProperty, Team team, int playerIndex, Function<Player, String> playerPropertyGetter) {
        return getPlayer(replayProperty, team, playerIndex).map(playerPropertyGetter).orElse("");
    }

    private String getPlayerLabelText(ObjectProperty<ReplayEntry> replayProperty, Team team, int playerIndex, ToIntFunction<Player> playerPropertyGetter) {
        return getPlayer(replayProperty, team, playerIndex).map(player -> NumberFormat.getIntegerInstance().format(playerPropertyGetter.applyAsInt(player))).orElse("");
    }

    private Optional<Player> getPlayer(ObjectProperty<ReplayEntry> replayProperty, Team team, int playerIndex) {
        ReplayEntry replay = replayProperty.get();
        if (replay == null || playerIndex >= replay.teamSize()) {
            return Optional.empty();
        }

        List<Player> players = replay.players(team);
        if (playerIndex >= players.size()) {
            return Optional.empty();
        }

        return Optional.of(players.get(playerIndex));
    }

    private void updateGoalTimeline(ReplayEntry newValue) {
        if (newValue == null) {
            return;
        }

        // Remove all rows except the header (row 0)
        this.goalGrid.getChildren().removeIf(node -> {
            Integer row = GridPane.getRowIndex(node);
            return row != null && row > 0;
        });

        List<Goal> goals = newValue.goals();
        Replay replay = newValue.getReplay();
        for (int i = 0; i < goals.size(); i++) {
            Goal goal = goals.get(i);
            long time = goal.timestamp(replay, TimeUnit.SECONDS);
            String styleClass = goal.team().name().toLowerCase() + "Team";
            String styleClassDark = styleClass + "Dark";

            Label timeLabel = new Label((time / 60) + ":" + TIME_FORMATTER.format((time % 60)));
            timeLabel.getStyleClass().add(styleClass);

            Label playerLabel = new Label(goal.playerName());
            playerLabel.getStyleClass().add(styleClassDark);

            this.goalGrid.add(timeLabel, 0, i + 1);
            this.goalGrid.add(playerLabel, 1, i + 1);
        }
    }

    @FXML
    private void closePanel(ActionEvent event) {
        App.getInstance().setDetailedReplay(null);
    }

}
