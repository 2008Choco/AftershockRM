package wtf.choco.aftershock.structure;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import wtf.choco.aftershock.replay.Replay;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class ReplayBin implements Iterable<Replay> {

    private final UUID uuid;
    private final ObservableList<ReplayEntry> replays;
    private final Map<String, Replay> byId;

    private String name;

    public ReplayBin(UUID uuid, String name, Collection<ReplayEntry> replays) {
        this.uuid = uuid;
        this.name = name;
        this.replays = FXCollections.observableArrayList(replays);
        this.byId = new HashMap<>(replays.size());

        for (ReplayEntry replay : replays) {
            this.byId.put(replay.getReplay().getId(), replay.getReplay());
        }
    }

    public ReplayBin(UUID uuid, String name, int size) {
        this.uuid = uuid;
        this.name = name;
        this.replays = FXCollections.observableArrayList();
        this.byId = new HashMap<>(size);
    }

    public ReplayBin(UUID uuid, String name) {
        this(uuid, name, 0);
    }

    public UUID getUUID() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public void addReplay(Replay replay) {
        this.replays.add(replay.getEntryData());
        this.byId.put(replay.getId(), replay);
    }

    public void removeReplay(Replay replay) {
        this.replays.removeIf(r -> r.getReplay() == replay);
        this.byId.remove(replay.getId());
    }

    public Replay getReplayById(String id) {
        return byId.get(id);
    }

    public List<ReplayEntry> getReplays() {
        return Collections.unmodifiableList(replays);
    }

    public ObservableList<ReplayEntry> getObservableList() {
        return replays;
    }

    public void clear() {
        this.replays.clear();
        this.byId.clear();
    }

    public int size() {
        return replays.size();
    }

    @Override
    public Iterator<Replay> iterator() {
        return byId.values().iterator();
    }

}
