package wtf.choco.aftershock.structure;

import java.util.AbstractList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import wtf.choco.aftershock.replay.Replay;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class ReplayBin extends AbstractList<ReplayEntry> {

    private final UUID uuid;
    private final ObservableList<ReplayEntry> replays;
    private final Map<String, ReplayEntry> byId;

    private String name;

    public ReplayBin(UUID uuid, String name, Collection<ReplayEntry> replays) {
        this.uuid = uuid;
        this.name = name;
        this.replays = FXCollections.observableArrayList(replays);
        this.byId = new HashMap<>(replays.size());

        for (ReplayEntry replay : replays) {
            this.byId.put(replay.getReplay().getId(), replay);
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

    public void addReplay(ReplayEntry replay) {
        this.replays.add(replay);
        this.byId.put(replay.getReplay().getId(), replay);
    }

    public void addReplay(Replay replay) {
        this.addReplay(new ReplayEntry(replay));
    }

    public void removeReplay(ReplayEntry replay) {
        this.removeReplay(replay.getReplay());
    }

    public void removeReplay(Replay replay) {
        this.replays.removeIf(r -> r.getReplay() == replay);
        this.byId.remove(replay.getId());
    }

    public ReplayEntry getReplayById(String id) {
        return byId.get(id);
    }

    public ObservableList<ReplayEntry> getObservableList() {
        return replays;
    }

    @Override
    public ReplayEntry get(int index) {
        return replays.get(index);
    }

    @Override
    public void clear() {
        this.replays.clear();
        this.byId.clear();
    }

    @Override
    public int size() {
        return replays.size();
    }

    @Override
    public Iterator<ReplayEntry> iterator() {
        return replays.iterator();
    }

}
