package wtf.choco.aftershock.structure;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.scene.Parent;
import wtf.choco.aftershock.controller.InfoPanelController;
import wtf.choco.aftershock.replay.AftershockData;
import wtf.choco.aftershock.replay.Goal;
import wtf.choco.aftershock.replay.IReplay;
import wtf.choco.aftershock.replay.Player;
import wtf.choco.aftershock.replay.Replay;
import wtf.choco.aftershock.replay.Team;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;

public class ReplayEntry implements IReplay {

    private Parent infoPanel;

    private final Path liveReplayPath;
    private final Path backupReplayPath;
    private final Path headerPath;
    private final Replay replayData;
    private final AftershockData aftershockData;

    public ReplayEntry(Path liveReplayPath, Path backupReplayPath, Path headerPath, Replay replayData, AftershockData aftershockData) {
        this.liveReplayPath = liveReplayPath;
        this.backupReplayPath = backupReplayPath;
        this.headerPath = headerPath;
        this.replayData = replayData;
        this.aftershockData = aftershockData;
    }

    public Path getLiveReplayPath() {
        return liveReplayPath;
    }

    public Path getBackupReplayPath() {
        return backupReplayPath;
    }

    public Path getHeaderPath() {
        return headerPath;
    }

    @Override
    public String id() {
        return replayData.id();
    }

    @Override
    public String name() {
        return replayData.name();
    }

    @Override
    public String playerName() {
        return replayData.playerName();
    }

    @Override
    public String mapId() {
        return replayData.mapId();
    }

    @Override
    public int teamSize() {
        return replayData.teamSize();
    }

    @Override
    public Map<Team, Integer> score() {
        return replayData.score();
    }

    @Override
    public int score(Team team) {
        return replayData.score(team);
    }

    @Override
    public int duration() {
        return replayData.duration();
    }

    @Override
    public int duration(TimeUnit unit) {
        return replayData.duration(unit);
    }

    @Override
    public double framesPerSecond() {
        return replayData.framesPerSecond();
    }

    @Override
    public LocalDateTime date() {
        return replayData.date();
    }

    @Override
    public List<Player> players() {
        return replayData.players();
    }

    @Override
    public List<Goal> goals() {
        return replayData.goals();
    }

    public Parent getInfoPanel(ResourceBundle resources) {
        if (infoPanel == null) {
            this.infoPanel = InfoPanelController.createInfoPanelFor(replayData, resources);
        }

        return infoPanel;
    }

    public AftershockData getAftershockData() {
        return aftershockData;
    }

    public void setLoaded(boolean loaded) {
        this.aftershockData.setLoaded(loaded);
    }

    public boolean isLoaded() {
        return aftershockData.isLoaded();
    }

    public BooleanProperty loadedProperty() {
        return aftershockData.loadedProperty();
    }

    public void setComments(String comments) {
        this.aftershockData.setComments(comments);
    }

    public String getComments() {
        return aftershockData.getComments();
    }

    public StringProperty commentsProperty() {
        return aftershockData.commentsProperty();
    }

    public void addTag(Tag tag) {
        this.aftershockData.addTag(tag);
    }

    public ObservableList<Tag> getTags() {
        return aftershockData.getTags();
    }

    public ListProperty<Tag> tagsProperty() {
        return aftershockData.tagsProperty();
    }

}
