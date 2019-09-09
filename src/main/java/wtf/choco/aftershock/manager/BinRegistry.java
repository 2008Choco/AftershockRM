package wtf.choco.aftershock.manager;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import wtf.choco.aftershock.structure.ReplayBin;

import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

public class BinRegistry  {

    public static final ReplayBin GLOBAL_BIN = new ReplayBin(UUID.randomUUID(), "Global");

    private final ObservableMap<String, ReplayBin> bins = FXCollections.observableHashMap();

    public BinRegistry() {
        this.addBin(GLOBAL_BIN);
    }

    public ReplayBin createBin(String name) {
        if (name == null || (name = name.toLowerCase()).equals("global")) {
            throw new IllegalStateException("'Global' is a reserved bin identifier");
        }

        ReplayBin bin = new ReplayBin(UUID.randomUUID(), name);
        this.bins.put(name.toLowerCase(), bin);
        return bin;
    }

    public void addBin(ReplayBin bin) {
        this.bins.put(bin.getName().toLowerCase(), bin);
    }

    public ReplayBin getBin(String name) {
        return bins.get(name.toLowerCase());
    }

    public void deleteBin(String name) {
        this.bins.remove(name.toLowerCase());
    }

    public void deleteBin(ReplayBin bin) {
        this.deleteBin(bin.getName().toLowerCase());
    }

    public void clearBins(boolean clearGlobal) {
        this.bins.values().forEach(ReplayBin::clear);
        if (clearGlobal) {
            GLOBAL_BIN.clear();
        }
    }

    public void deleteBins(boolean deleteGlobal) {
        this.clearBins(deleteGlobal);
        this.bins.clear();

        if (!deleteGlobal) {
            this.addBin(GLOBAL_BIN);
        }
    }

    public Collection<ReplayBin> getBins() {
        return Collections.unmodifiableCollection(bins.values());
    }

    public ObservableMap<String, ReplayBin> getObservableBins() {
        return bins;
    }

}
