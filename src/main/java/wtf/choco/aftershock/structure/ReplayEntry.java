package wtf.choco.aftershock.structure;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.scene.Parent;
import wtf.choco.aftershock.App;
import wtf.choco.aftershock.controller.InfoPanelController;
import wtf.choco.aftershock.files.AftershockFileOperations;
import wtf.choco.aftershock.replay.ReplayMetadata;
import wtf.choco.aftershock.replay.Goal;
import wtf.choco.aftershock.replay.IReplay;
import wtf.choco.aftershock.replay.Player;
import wtf.choco.aftershock.replay.Replay;
import wtf.choco.aftershock.replay.Team;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ReplayEntry implements IReplay {

    private Parent infoPanel;

    private final Path liveReplayPath;
    private final Path backupReplayPath;
    private final Path headerPath;
    private final Replay replayData;
    private final ReplayMetadata metadata;

    public ReplayEntry(Path liveReplayPath, Path backupReplayPath, Path headerPath, Replay replayData, ReplayMetadata metadata) {
        this.liveReplayPath = liveReplayPath;
        this.backupReplayPath = backupReplayPath;
        this.headerPath = headerPath;
        this.replayData = replayData;
        this.metadata = metadata;
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

    public static void performBulkLoadOperation(Collection<ReplayEntry> replays, boolean newLoadedState) {
        List<ReplayEntry> replaysToUpdate = replays.stream().filter(replay -> replay.isLoaded() != newLoadedState).collect(Collectors.toCollection(ArrayList::new));
        if (replaysToUpdate.isEmpty()) {
            return;
        }

        AftershockFileOperations fileOperations = App.getInstance().getFileOperations();
        CompletionStage<Void> future;
        if (newLoadedState) {
            future = fileOperations.restoreReplayBackups(replaysToUpdate.stream().map(ReplayEntry::getBackupReplayPath).toList());
        } else {
            future = fileOperations.deleteReplays(replaysToUpdate.stream().map(ReplayEntry::getLiveReplayPath).toList(), false);
        }
        future.thenRunAsync(() -> replaysToUpdate.forEach(replay -> replay.setLoaded(newLoadedState)), Platform::runLater);
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

}
