package wtf.choco.aftershock.structure;

import java.util.Optional;

import wtf.choco.aftershock.replay.Replay;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class ReplayEntry {

    private final Replay replay;

    private BooleanProperty loaded = new SimpleBooleanProperty(true);
    private String comments;

    public ReplayEntry(Replay replay, String comments) {
        this.replay = replay;
        this.comments = comments;
    }

    public ReplayEntry(Replay replay) {
        this(replay, null);
    }

    public Replay getReplay() {
        return replay;
    }

    public void setLoaded(boolean loaded) {
        this.loaded.set(loaded);
    }

    public boolean isLoaded() {
        return loaded.get();
    }

    public BooleanProperty loadedProperty() {
        return loaded;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public Optional<String> getComments() {
        return Optional.ofNullable(comments);
    }

}
