package wtf.choco.aftershock.structure;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import wtf.choco.aftershock.App;
import wtf.choco.aftershock.util.Preconditions;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ReplayBin implements Iterable<ReplayEntry> {

    public static final Image BIN_GRAPHIC_EMPTY = new Image(App.class.getResourceAsStream("/icons/folder.png"));
    public static final Image BIN_GRAPHIC_FULL = new Image(App.class.getResourceAsStream("/icons/folder-full.png"));

    private static boolean globalBinCreated = false;

    private final StringProperty name;
    private final ReadOnlyBooleanProperty global;
    private final BooleanProperty hidden;
    private final BooleanProperty active = new SimpleBooleanProperty(this, "active", false);

    private final ListProperty<ReplayEntry> replays;
    private final MapProperty<String, ReplayEntry> replaysById;

    private final UUID uuid;

    public ReplayBin(UUID uuid, String name, Collection<ReplayEntry> replays, boolean global) {
        Preconditions.checkState(!global || !globalBinCreated, "Cannot create more than one global bin. Refer to BinRegistry#getGlobalBin()");

        this.uuid = uuid;

        this.name = new SimpleStringProperty(this, "name", name);
        this.global = new SimpleBooleanProperty(this, "global", global);
        this.hidden = new SimpleBooleanProperty(this, "hidden", false);

        this.replays = new SimpleListProperty<>(this, "replays", FXCollections.observableArrayList(replays));
        Map<String, ReplayEntry> byIdMap = replays.stream().collect(Collectors.toMap(ReplayEntry::id, Function.identity()));
        this.replaysById = new SimpleMapProperty<>(this, "replaysById", FXCollections.observableMap(byIdMap));

        // Keep the map values in sync with the list values
        this.replays.addListener((ListChangeListener<? super ReplayEntry>) change -> {
            while (change.next()) {
                change.getAddedSubList().forEach(entry -> replaysById.put(entry.id(), entry));
                change.getRemoved().forEach(entry -> replaysById.remove(entry.id()));
            }
        });

        if (global) {
            globalBinCreated = true;
        }
    }

    public ReplayBin(UUID uuid, String name, boolean global) {
        this(uuid, name, Collections.emptyList(), global);
    }

    public ReplayBin(UUID uuid, String name) {
        this(uuid, name, false);
    }

    public ReplayBin(ReplayBin bin) {
        this(UUID.randomUUID(), App.getInstance().getBinRegistry().getSafeName(bin.getName()), bin.getReplays(), false);
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

    public void setActive(boolean active) {
        this.activeProperty().set(active);
    }

    public boolean isActive() {
        return activeProperty().get();
    }

    public BooleanProperty activeProperty() {
        return active;
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
