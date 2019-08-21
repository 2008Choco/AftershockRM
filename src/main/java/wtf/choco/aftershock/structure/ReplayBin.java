package wtf.choco.aftershock.structure;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ReplayBin implements Iterable<ReplayEntry> {

    private final int id;
    private final Set<ReplayEntry> replays;

    private String name;

    public ReplayBin(int id, String name, Collection<ReplayEntry> replays) {
        this.id = id;
        this.name = name;
        this.replays = new HashSet<>(replays);
    }

    public ReplayBin(int id, String name, int size) {
        this.id = id;
        this.name = name;
        this.replays = new HashSet<>(size);
    }

    public ReplayBin(int id, String name) {
        this(id, name, 0);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void addReplay(ReplayEntry replay) {
        this.replays.add(replay);
    }

    public void removeReplay(ReplayEntry replay) {
        this.replays.remove(replay);
    }

    public Collection<ReplayEntry> getReplays() {
        return Collections.unmodifiableCollection(replays);
    }

    @Override
    public Iterator<ReplayEntry> iterator() {
        return replays.iterator();
    }

}
