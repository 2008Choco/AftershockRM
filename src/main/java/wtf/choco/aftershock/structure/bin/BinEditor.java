package wtf.choco.aftershock.structure.bin;

import wtf.choco.aftershock.App;
import wtf.choco.aftershock.controller.AppController;
import wtf.choco.aftershock.manager.BinRegistry;
import wtf.choco.aftershock.structure.ReplayBin;
import wtf.choco.aftershock.structure.ReplayEntry;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.Node;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

public class BinEditor {

    private ReplayBin displayed;

    private final AppController controller;
    private final ObservableList<Node> listed;
    private final BinSelectionModel selectionModel = new BinSelectionModel();
    private final TableView<ReplayEntry> replayTable;
    private final VBox node;

    private final ListChangeListener<ReplayEntry> binChangeListener;

    public BinEditor(AppController controller, VBox node, VBox list, ListChangeListener<ReplayEntry> binChangeListener) {
        this.controller = controller;
        this.replayTable = controller.getReplayTable();
        this.node = node;
        this.listed = list.getChildren();
        this.binChangeListener = binChangeListener;

        BinRegistry binRegistry = App.getInstance().getBinRegistry();
        binRegistry.getBins().forEach(b -> listed.add(b.getDisplay()));

        // Listeners
        binRegistry.getBins().addListener((ListChangeListener<ReplayBin>) c -> {
            if (!c.next()) {
                return;
            }

            if (c.wasAdded()) {
                c.getAddedSubList().forEach(b -> listed.add(b.getDisplay()));
            } else {
                c.getRemoved().forEach(b -> listed.remove(b.getDisplay()));
            }
        });

        this.selectionModel.getSelectedItems().addListener((ListChangeListener<ReplayBin>) c -> {
            if (!c.next()) {
                return;
            }

            if (c.wasAdded()) {
                c.getAddedSubList().forEach(b -> b.getDisplay().getStyleClass().add("bin-display-selected"));
            } else if (c.wasRemoved()) {
                c.getRemoved().forEach(b -> b.getDisplay().getStyleClass().remove("bin-display-selected"));
            }
        });
    }

    public VBox getNode() {
        return node;
    }

    public void display(ReplayBin bin) {
        this.displayed = bin;

        if (bin == null) {
            this.replayTable.setItems(FXCollections.emptyObservableList());
            return;
        }

        FilteredList<ReplayEntry> entries = new FilteredList<>(bin.getObservableList());
        entries.setPredicate(controller.getTableFilter());
        this.replayTable.setItems(entries);
        this.selectionModel.select(displayed);

        // Ensure there is only ever one instance of the listener by removing it first
        entries.removeListener(binChangeListener);
        entries.addListener(binChangeListener);
    }

    public void clearDisplay() {
        this.display(null);
    }

    public ReplayBin getDisplayed() {
        return displayed;
    }

    public BinSelectionModel getSelectionModel() {
        return selectionModel;
    }

}
