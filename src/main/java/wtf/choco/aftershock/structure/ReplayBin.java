package wtf.choco.aftershock.structure;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import wtf.choco.aftershock.App;
import wtf.choco.aftershock.manager.BinRegistry;
import wtf.choco.aftershock.replay.Replay;
import wtf.choco.aftershock.structure.bin.BinDisplayComponent;
import wtf.choco.aftershock.util.Preconditions;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;

public class ReplayBin implements Iterable<Replay> {

    public static final Image BIN_GRAPHIC_EMPTY = new Image(App.class.getResourceAsStream("/icons/folder.png"));
    public static final Image BIN_GRAPHIC_FULL = new Image(App.class.getResourceAsStream("/icons/folder-full.png"));

    private final UUID uuid;
    private final ObservableList<ReplayEntry> replays;
    private final Map<String, Replay> byId;
    private final boolean globalBin;
    private final BinDisplayComponent display;

    private String name;
    private boolean hidden;

    public ReplayBin(UUID uuid, String name, Collection<ReplayEntry> replays, boolean isGlobalBin) {
        Preconditions.checkState(!isGlobalBin || (isGlobalBin && BinRegistry.GLOBAL_BIN == null), "Cannot create more than one global bin. Refer to BinRegistry.GLOBAL_BIN");

        this.uuid = uuid;
        this.name = name;
        this.replays = FXCollections.observableArrayList(replays);
        this.byId = new HashMap<>(replays.size());
        this.globalBin = isGlobalBin;
        this.display = new BinDisplayComponent(this, replays.isEmpty() ? BIN_GRAPHIC_EMPTY : BIN_GRAPHIC_FULL);

        for (ReplayEntry replay : replays) {
            this.byId.put(replay.getReplay().getId(), replay.getReplay());
        }
    }

    public ReplayBin(UUID uuid, String name, boolean isGlobalBin) {
        Preconditions.checkState(!isGlobalBin || (isGlobalBin && BinRegistry.GLOBAL_BIN == null), "Cannot create more than one global bin. Refer to BinRegistry.GLOBAL_BIN");

        this.uuid = uuid;
        this.name = name;
        this.replays = FXCollections.observableArrayList();
        this.byId = new HashMap<>(0);
        this.globalBin = isGlobalBin;
        this.display = new BinDisplayComponent(this, BIN_GRAPHIC_EMPTY);
    }

    public ReplayBin(UUID uuid, String name, Collection<ReplayEntry> replays) {
        this(uuid, name, replays, false);
    }

    public ReplayBin(UUID uuid, String name) {
        this(uuid, name, false);
    }

    public UUID getUUID() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.display.updateName();
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public boolean isGlobalBin() {
        return globalBin;
    }

    public BinDisplayComponent getDisplay() {
        return display;
    }

    public void addReplay(Replay replay) {
        this.replays.add(replay.getEntryData());
        this.byId.put(replay.getId(), replay);
    }

    public boolean hasReplay(Replay replay) {
        return byId.containsValue(replay);
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
