package wtf.choco.aftershock.manager;

import wtf.choco.aftershock.replay.Replay;
import wtf.choco.aftershock.structure.ReplayEntry;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableView;

public class ReplayManager {

    private final ObservableList<ReplayEntry> replayEntries = FXCollections.observableArrayList();

    public void attachTable(TableView<ReplayEntry> table) {
        table.setItems(replayEntries);
    }

    public void addReplay(ReplayEntry replay) {
        this.replayEntries.add(replay);
    }

    public void addReplay(Replay replay) {
        this.addReplay(new ReplayEntry(replay));
    }

    public void removeReplay(ReplayEntry replay) {
        this.replayEntries.remove(replay);
    }

    public void removeReplay(Replay replay) {
        this.replayEntries.removeIf(r -> r.getReplay() == replay);
    }

    public void clearReplays() {
        this.replayEntries.clear();
    }

    public int getReplayCount() {
        return replayEntries.size();
    }

}
