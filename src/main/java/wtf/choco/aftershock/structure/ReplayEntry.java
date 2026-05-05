package wtf.choco.aftershock.structure;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import wtf.choco.aftershock.App;
import wtf.choco.aftershock.files.AftershockFileOperations;
import wtf.choco.aftershock.replay.ReplayMetadata;
import wtf.choco.aftershock.replay.Goal;
import wtf.choco.aftershock.replay.IReplay;
import wtf.choco.aftershock.replay.Player;
import wtf.choco.aftershock.replay.Replay;
import wtf.choco.aftershock.replay.Team;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class ReplayEntry implements IReplay {

    private final Path liveReplayPath;
    private final Path unloadedReplayPath;
    private final Replay replayData;
    private final ReplayMetadata metadata;

    public ReplayEntry(Path liveReplayPath, Path unloadedReplayPath, Replay replayData, ReplayMetadata metadata) {
        this.liveReplayPath = liveReplayPath;
        this.unloadedReplayPath = unloadedReplayPath;
        this.replayData = replayData;
        this.metadata = metadata;

        this.metadata.loadedProperty().addListener((_, _, newValue) -> {
            AftershockFileOperations fileOperations = App.getInstance().getFileOperations();

            CompletionStage<Void> future;
            if (newValue) {
                future = fileOperations.restoreUnloadedReplays(List.of(unloadedReplayPath));
            } else {
                future = fileOperations.unloadReplays(List.of(liveReplayPath));
            }

            future.exceptionally(e -> {
                App.LOGGER.log(Level.SEVERE, "Failed to " + (newValue ? "restore" : "unload") + " replay: " + replayData.id() + " (\"" + replayData.name() + "\")", e);
                return null;
            });
        });
    }

    public Path getLiveReplayPath() {
        return liveReplayPath;
    }

    public Path getUnloadedReplayPath() {
        return unloadedReplayPath;
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

    public Replay getReplay() {
        return replayData;
    }

    public ReplayMetadata getMetadata() {
        return metadata;
    }

    public void setLoaded(boolean loaded) {
        this.getMetadata().setLoaded(loaded);
    }

    public boolean isLoaded() {
        return getMetadata().isLoaded();
    }

    public BooleanProperty loadedProperty() {
        return getMetadata().loadedProperty();
    }

    public void setComments(String comments) {
        this.getMetadata().setComment(comments);
    }

    public String getComments() {
        return getMetadata().getComment();
    }

    public StringProperty commentsProperty() {
        return getMetadata().commentProperty();
    }

    public void addTag(Tag tag) {
        this.getMetadata().addTag(tag);
    }

    public ObservableList<Tag> getTags() {
        return getMetadata().getTags();
    }

    public ListProperty<Tag> tagsProperty() {
        return getMetadata().tagsProperty();
    }

    @Override
    public int hashCode() {
        return id().hashCode();
    }

    @Override
    public boolean equals(Object object) {
        return object == this || (object instanceof IReplay other && id().equals((other).id()));
    }

}
