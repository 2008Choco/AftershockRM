package wtf.choco.aftershock.util;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import wtf.choco.aftershock.structure.ReplayBin;
import wtf.choco.aftershock.structure.ReplayEntry;

import java.util.function.Predicate;

public final class ReplayTableFilter implements Predicate<ReplayEntry> {

    private final ObjectProperty<ReplayBin> replayBin;
    private final StringProperty searchTerm;

    public ReplayTableFilter(ReplayBin initialReplayBin) {
        this.replayBin = new SimpleObjectProperty<>(this, "replayBin", initialReplayBin);
        this.searchTerm = new SimpleStringProperty(this, "searchTerm");
    }

    public ReplayBin getReplayBin() {
        return replayBinProperty().get();
    }

    public ObjectProperty<ReplayBin> replayBinProperty() {
        return replayBin;
    }

    public String getSearchTerm() {
        return searchTermProperty().get();
    }

    public StringProperty searchTermProperty() {
        return searchTerm;
    }

    @Override
    public boolean test(ReplayEntry replay) {
        ReplayBin bin = getReplayBin();
        if (bin != null && !bin.getReplays().contains(replay)) {
            return false;
        }

        String searchTerm = getSearchTerm();
        if (searchTerm != null && !searchTerm.isBlank()) {
            String lowercaseSearchTerm = searchTerm.toLowerCase();
            return replay.name().toLowerCase().contains(lowercaseSearchTerm) || replay.id().toLowerCase().contains(lowercaseSearchTerm);
        }

        return true;
    }

}
