package wtf.choco.aftershock.structure.bin;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.MultipleSelectionModel;
import wtf.choco.aftershock.App;
import wtf.choco.aftershock.manager.BinRegistry;
import wtf.choco.aftershock.structure.ReplayBin;

public class BinSelectionModel extends MultipleSelectionModel<ReplayBin> {

    private final ObservableList<Integer> selectedIndices = FXCollections.observableArrayList();
    private final ObservableList<ReplayBin> selected = FXCollections.observableArrayList();

    private final BinRegistry binRegistry = App.getInstance().getBinRegistry();

    @Override
    public ObservableList<Integer> getSelectedIndices() {
        return selectedIndices;
    }

    @Override
    public ObservableList<ReplayBin> getSelectedItems() {
        return selected;
    }

    @Override
    public void selectIndices(int index, int... indices) {
        this.select(index);
        for (int additionalIndex : indices) {
            this.select(additionalIndex);
        }
    }

    @Override
    public void selectAll() {
        this.selected.setAll(binRegistry.getBins());
    }

    @Override
    public void selectFirst() {
        if (binRegistry.getBinCount() > 0) {
            this.selected.add(binRegistry.getBin(0));
        }
    }

    @Override
    public void selectLast() {
        if (binRegistry.getBinCount() > 0) {
            this.selected.add(binRegistry.getBin(binRegistry.getBinCount() - 1));
        }
    }

    @Override
    public void clearAndSelect(int index) {
        this.selected.clear();
        this.select(index);
    }

    public void clearAndSelect(ReplayBin bin) {
        this.clearSelection();
        this.select(bin);
    }

    @Override
    public void select(int index) {
        if (index < binRegistry.getBinCount()) {
            this.selected.add(binRegistry.getBin(index));
            this.selectedIndices.add(index);
        }
    }

    @Override
    public void select(ReplayBin obj) {
        int index = 0;
        for (ReplayBin bin : binRegistry.getBins()) {
            if (bin == obj) {
                this.selected.add(bin);
                this.selectedIndices.add(index);
                return;
            }

            index++;
        }
    }

    @Override
    public void clearSelection(int index) {
        if (index < binRegistry.getBinCount()) {
            this.selected.remove(binRegistry.getBin(index));
            this.selectedIndices.remove((Integer) index);
        }
    }

    public void clearSelection(ReplayBin obj) {
        int index = 0;
        for (ReplayBin bin : binRegistry.getBins()) {
            if (bin == obj) {
                this.selected.remove(bin);
                this.selectedIndices.remove((Integer) index);
                return;
            }

            index++;
        }
    }

    @Override
    public void clearSelection() {
        this.selected.clear();
        this.selectedIndices.clear();
    }

    @Override
    public boolean isSelected(int index) {
        return selectedIndices.contains(index);
    }

    public boolean isSelected(ReplayBin obj) {
        return selected.contains(obj);
    }

    @Override
    public boolean isEmpty() {
        return selected.isEmpty();
    }

    @Override
    public void selectPrevious() {
        throw new UnsupportedOperationException("selectPrevious() is unsupported");
    }

    @Override
    public void selectNext() {
        throw new UnsupportedOperationException("selectNext() is unsupported");
    }

}
