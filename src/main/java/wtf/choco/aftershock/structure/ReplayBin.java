package wtf.choco.aftershock.structure;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import wtf.choco.aftershock.App;
import wtf.choco.aftershock.structure.bin.BinDisplayComponent;
import wtf.choco.aftershock.util.Preconditions;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ReplayBin implements Iterable<ReplayEntry> {

    public static final Image BIN_GRAPHIC_EMPTY = new Image(App.class.getResourceAsStream("/icons/folder.png"));
    public static final Image BIN_GRAPHIC_FULL = new Image(App.class.getResourceAsStream("/icons/folder-full.png"));

    private static boolean globalBinCreated = false;

    private final UUID uuid;
    private final ObservableList<ReplayEntry> replays;
    private final Map<String, ReplayEntry> byId;
    private final boolean globalBin;
    private final BinDisplayComponent display;

    private String name;
    private boolean hidden;

    public ReplayBin(UUID uuid, String name, Collection<ReplayEntry> replays, boolean isGlobalBin) {
        Preconditions.checkState(!isGlobalBin || !globalBinCreated, "Cannot create more than one global bin. Refer to BinRegistry#getGlobalBin()");

        this.uuid = uuid;
        this.name = name;
        this.replays = FXCollections.observableArrayList(replays);
        this.byId = new HashMap<>(replays.size());
        this.globalBin = isGlobalBin;
        this.display = new BinDisplayComponent(this, replays.isEmpty() ? BIN_GRAPHIC_EMPTY : BIN_GRAPHIC_FULL);

        for (ReplayEntry replay : replays) {
            this.byId.put(replay.id(), replay);
        }

        globalBinCreated = true;
    }

    public ReplayBin(UUID uuid, String name, boolean isGlobalBin) {
        Preconditions.checkState(!isGlobalBin || !globalBinCreated, "Cannot create more than one global bin. Refer to BinRegistry#getGlobalBin()");

        this.uuid = uuid;
        this.name = name;
        this.replays = FXCollections.observableArrayList();
        this.byId = new HashMap<>(0);
        this.globalBin = isGlobalBin;
        this.display = new BinDisplayComponent(this, BIN_GRAPHIC_EMPTY);

        globalBinCreated = true;
    }

    public ReplayBin(UUID uuid, String name) {
        this(uuid, name, false);
    }

    public ReplayBin(ReplayBin bin) {
        this(UUID.randomUUID(), App.getInstance().getBinRegistry().getSafeName(bin.name), bin.replays, false);
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

    public void addReplay(ReplayEntry replay) {
        this.replays.add(replay);
        this.byId.put(replay.id(), replay);
    }

    public boolean hasReplay(ReplayEntry replay) {
        return byId.containsValue(replay);
    }

    public boolean hasReplay(String id) {
        return byId.containsKey(id);
    }

    public void removeReplay(ReplayEntry replay) {
        this.replays.remove(replay);
        this.byId.remove(replay.id());
    }

    public ReplayEntry getReplayById(String id) {
        return byId.get(id);
    }

    public List<ReplayEntry> getReplays() {
        return Collections.unmodifiableList(replays);
    }

    public ObservableList<ReplayEntry> getReplaysObservable() {
        return replays;
    }

    public boolean isEmpty() {
        return replays.isEmpty();
    }

    public void clear() {
        this.replays.clear();
        this.byId.clear();
    }

    public int size() {
        return replays.size();
    }

    @Override
    public Iterator<ReplayEntry> iterator() {
        return byId.values().iterator();
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof ReplayBin other && uuid.equals(other.uuid));
    }

}
