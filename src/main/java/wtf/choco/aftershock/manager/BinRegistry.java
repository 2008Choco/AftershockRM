package wtf.choco.aftershock.manager;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import wtf.choco.aftershock.structure.ReplayBin;

public class BinRegistry  {

    public static final ReplayBin GLOBAL_BIN = new ReplayBin(UUID.randomUUID(), "global");


    private final Map<String, ReplayBin> bins = new HashMap<>();

    public BinRegistry() {
        this.addBin(GLOBAL_BIN);
    }

    public ReplayBin createBin(String name) {
        if (name == null || (name = name.toLowerCase()).equals("global")) {
            throw new IllegalStateException("'global' is a reserved bin identified");
        }

        ReplayBin bin = new ReplayBin(UUID.randomUUID(), name);
        this.bins.put(name, bin);
        return bin;
    }

    public void addBin(ReplayBin bin) {
        this.bins.put(bin.getName(), bin);
    }

    public ReplayBin getBin(String name) {
        return bins.get(name);
    }

    public void deleteBin(String name) {
        this.bins.remove(name);
    }

    public void deleteBin(ReplayBin bin) {
        this.deleteBin(bin.getName());
    }

    public void clearBins(boolean keepGlobal) {
        this.bins.values().forEach(ReplayBin::clear);
        this.bins.clear();

        if (keepGlobal) {
            this.addBin(GLOBAL_BIN);
        }
    }

    public Collection<ReplayBin> getBins() {
        return Collections.unmodifiableCollection(bins.values());
    }

}
