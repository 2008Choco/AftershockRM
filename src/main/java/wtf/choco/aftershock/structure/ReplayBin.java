package wtf.choco.aftershock.structure;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import wtf.choco.aftershock.App;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ReplayBin implements Iterable<ReplayEntry> {

    private static final UUID GLOBAL_BIN_UUID = UUID.nameUUIDFromBytes("aftershock_global_bin".getBytes());
    public static final ReplayBin GLOBAL = new ReplayBin(GLOBAL_BIN_UUID, "Global");

    private final ReadOnlyBooleanProperty global;

    private final UUID uuid;
    private final StringProperty name;
    private final BooleanProperty hidden;
    private final ListProperty<ReplayEntry> replays;
    private final Map<String, ReplayEntry> replaysById;

    public ReplayBin(UUID uuid, String name, boolean hidden, Collection<ReplayEntry> replays) {
        this.uuid = uuid;
        this.name = new SimpleStringProperty(this, "name", name);
        this.hidden = new SimpleBooleanProperty(this, "hidden", hidden);

        this.replays = new SimpleListProperty<>(this, "replays", FXCollections.observableArrayList(replays));
        this.replaysById = new HashMap<>(replays.stream().collect(Collectors.toMap(ReplayEntry::id, Function.identity())));

        // Keep the map values in sync with the list values
        this.replays.addListener((ListChangeListener<? super ReplayEntry>) change -> {
            while (change.next()) {
                change.getAddedSubList().forEach(entry -> replaysById.put(entry.id(), entry));
                change.getRemoved().forEach(entry -> replaysById.remove(entry.id()));
            }
        });

        this.global = new SimpleBooleanProperty(this, "global", uuid.equals(GLOBAL_BIN_UUID));
    }

    public ReplayBin(UUID uuid, String name) {
        this(uuid, name, false, Collections.emptyList());
    }

    public ReplayBin(ReplayBin bin) {
        this(UUID.randomUUID(), App.getInstance().getBinRegistry().getSafeName(bin.getName()), false, bin.getReplays());
    }

    public UUID getUUID() {
        return uuid;
    }

    public void setName(String name) {
        this.nameProperty().set(name);
    }

    public String getName() {
        return nameProperty().get();
    }

    public StringProperty nameProperty() {
        return name;
    }

    public boolean isGlobal() {
        return globalProperty().get();
    }

    public ReadOnlyBooleanProperty globalProperty() {
        return global;
    }

    public void setHidden(boolean hidden) {
        this.hiddenProperty().set(hidden);
    }

    public boolean isHidden() {
        return hiddenProperty().get();
    }

    public BooleanProperty hiddenProperty() {
        return hidden;
    }

    public ReplayEntry getReplay(String id) {
        return replaysById.get(id);
    }

    public boolean containsReplay(String id) {
        return replaysById.containsKey(id);
    }

    public ObservableList<ReplayEntry> getReplays() {
        return replaysProperty().get();
    }

    public ListProperty<ReplayEntry> replaysProperty() {
        return replays;
    }

    public boolean isEmpty() {
        return replays.isEmpty();
    }

    public void clear() {
        this.replays.clear();
    }

    public int size() {
        return replays.size();
    }

    @Override
    public Iterator<ReplayEntry> iterator() {
        return replaysProperty().iterator();
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
