package wtf.choco.aftershock.manager;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import wtf.choco.aftershock.structure.ReplayBin;

import java.util.Collection;
import java.util.UUID;

public class BinRegistry  {

    private final ListProperty<ReplayBin> bins = new SimpleListProperty<>(this, "bins", FXCollections.observableArrayList(ReplayBin.GLOBAL));

    public ReplayBin createBin(String name) {
        if (name == null || name.equalsIgnoreCase("global")) {
            throw new IllegalStateException("'Global' is a reserved bin identifier");
        }

        for (ReplayBin bin : getBins()) {
            if (bin.getName().equalsIgnoreCase(name)) {
                return null;
            }
        }

        ReplayBin bin = new ReplayBin(UUID.randomUUID(), name);
        this.addBin(bin);
        return bin;
    }

    public void addBin(ReplayBin bin) {
        this.getBins().add(bin);
    }

    public void addBins(Collection<ReplayBin> bins) {
        this.getBins().addAll(bins);
    }

    public ReplayBin getBin(int index) {
        return getBins().get(index);
    }

    public ReplayBin getBin(String name) {
        for (ReplayBin bin : bins) {
            if (bin.getName().equalsIgnoreCase(name)) {
                return bin;
            }
        }

        return null;
    }

    public void deleteBin(ReplayBin bin) {
        this.getBins().remove(bin);
    }

    public void clearBins(boolean includeGlobal) {
        for (ReplayBin bin : getBins()) {
            if (!includeGlobal && bin.isGlobal()) {
                continue;
            }

            bin.clear();
        }
    }

    public void deleteBins(boolean includeGlobal) {
        this.getBins().removeIf(bin -> includeGlobal || !bin.isGlobal());
    }

    public ObservableList<ReplayBin> getBins() {
        return bins.get();
    }

    public ListProperty<ReplayBin> binsProperty() {
        return bins;
    }

    public String getSafeName(String base) {
        int duplicateCount = 0;
        String result = base;

        do {
            result = (base + (duplicateCount++ >= 1 ? " (" + duplicateCount + ")" : ""));
        } while (getBin(result) != null);

        return result;
    }

}
