package wtf.choco.aftershock.structure;

import java.util.Optional;

import wtf.choco.aftershock.replay.Replay;

public class ReplayEntry {

    private final Replay replay;

    private boolean loaded = true;
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
        this.loaded = loaded;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public Optional<String> getComments() {
        return Optional.ofNullable(comments);
    }

}
