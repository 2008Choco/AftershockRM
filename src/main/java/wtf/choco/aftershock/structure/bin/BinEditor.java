package wtf.choco.aftershock.structure.bin;

import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

import wtf.choco.aftershock.App;
import wtf.choco.aftershock.controller.AppController;
import wtf.choco.aftershock.manager.BinRegistry;
import wtf.choco.aftershock.structure.ReplayBin;
import wtf.choco.aftershock.structure.ReplayEntry;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;

public class BinEditor {

    private ReplayBin displayed;

    private final App app;
    private final AppController controller;

    private final ObservableList<Node> listed;
    private final BinSelectionModel selectionModel = new BinSelectionModel();
    private final TableView<ReplayEntry> replayTable;
    private final VBox node;

    private final ObservableSet<ReplayBin> hidden = FXCollections.observableSet();
    private final Label hiddenLabel = new Label();

    public BinEditor(App app, AppController controller, VBox node, VBox list) {
        this.app = app;
        this.controller = controller;
        this.replayTable = controller.getReplayTable();
        this.node = node;
        this.listed = list.getChildren();

        BinRegistry binRegistry = app.getBinRegistry();
        binRegistry.getBins().forEach(b -> listed.add(b.getDisplay()));

        // Listeners
        binRegistry.getBins().addListener((ListChangeListener<ReplayBin>) c -> {
            if (!c.next()) {
                return;
            }

            if (c.wasAdded()) {
                c.getAddedSubList().forEach(b -> {
                    this.listed.add(b.getDisplay());
                    if (b.isHidden()) {
                        this.hideBin(b);
                    }
                });
            } else {
                c.getRemoved().forEach(b -> {
                    this.unhide(b);
                    this.listed.remove(b.getDisplay());
                });
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

        this.hiddenLabel.setAlignment(Pos.CENTER);
        this.hiddenLabel.setMaxWidth(100);
        this.hiddenLabel.setFont(Font.font(hiddenLabel.getFont().getFamily(), FontWeight.LIGHT, FontPosture.ITALIC, 10));

        this.hidden.addListener((SetChangeListener<ReplayBin>) c -> {
            ObservableList<Node> children = node.getChildren();

            int newSize = c.getSet().size();
            if (newSize == 0) {
                children.remove(1);
                return;
            }

            if (children.size() == 2) {
                children.add(1, hiddenLabel);
            }

            this.hiddenLabel.setText("(" + newSize + ") bins hidden");
        });
    }

    public VBox getNode() {
        return node;
    }

    public int indexOf(ReplayBin bin) {
        for (int i = 0; i < listed.size(); i++) {
            Node potentialBin = listed.get(i);
            if (!(potentialBin instanceof BinDisplayComponent)) {
                continue;
            }

            if (bin == ((BinDisplayComponent) potentialBin).getBin()) {
                return i;
            }
        }

        return -1;
    }

    public ReplayBin getBin(int index) {
        Node potentialBin = listed.get(index);
        return (potentialBin instanceof BinDisplayComponent) ? ((BinDisplayComponent) potentialBin).getBin() : null;
    }

    public void display(ReplayBin bin) {
        this.displayed = bin;

        if (bin == null) {
            this.replayTable.setItems(FXCollections.emptyObservableList());
            return;
        }

        ObservableList<ReplayEntry> entries = (controller.getTableFilter().isInvalid()) ? bin.getReplaysObservable() : new FilteredList<>(bin.getReplaysObservable(), controller.getTableFilter());
        this.replayTable.setItems(entries);
        this.selectionModel.select(displayed);
    }

    public void clearDisplay() {
        this.display(null);
    }

    public ReplayBin getDisplayed() {
        return displayed;
    }

    public boolean deleteBin(ReplayBin bin, boolean shouldAlert) {
        if (bin == null) {
            return false;
        }

        if (bin == BinRegistry.GLOBAL_BIN) {
            if (shouldAlert) {
                Toolkit.getDefaultToolkit().beep();
            }

            return false;
        }

        if (!bin.isEmpty() && shouldAlert) {
            Alert alert = new Alert(AlertType.WARNING);
            alert.setTitle("Confirm Bin Deletion");
            alert.setHeaderText("The bin selected for deletion contain at least one replay!");
            alert.setContentText("Deleting a bin is irreversible! Are you sure you want to delete: " + bin.getName() + "?");

            ButtonType buttonDelete = new ButtonType("Delete");
            ButtonType buttonCancel = new ButtonType("Cancel");
            alert.getButtonTypes().setAll(buttonDelete, buttonCancel);

            if (alert.showAndWait().orElse(buttonCancel) == buttonCancel) {
                return false;
            }
        }

        this.selectionModel.clearSelection(bin);
        this.hidden.remove(bin); // Just in case it's hidden
        this.app.getBinRegistry().deleteBin(bin);

        if (displayed == bin) {
            this.display(selectionModel.isEmpty() ? BinRegistry.GLOBAL_BIN : selectionModel.getSelectedItems().get(0));
        }

        return true;
    }

    public boolean deleteBins(Collection<ReplayBin> bins, boolean shouldAlert) {
        if (bins == null) {
            return false;
        }

        if (bins.size() == 1) {
            return deleteBin(getAtIndex(bins, 0), shouldAlert);
        }

        bins.removeIf(b -> b == BinRegistry.GLOBAL_BIN);
        boolean allEmpty = bins.stream().allMatch(ReplayBin::isEmpty);
        if (!allEmpty && shouldAlert) {
            String binNames = bins.stream().map(b -> '"' + b.getName() + '"').collect(Collectors.joining(","));

            Alert alert = new Alert(AlertType.WARNING);
            alert.setTitle("Confirm Bin Deletion");
            alert.setHeaderText("One or more of the bins selected for deletion contain at least one replay!");
            alert.setContentText("Deleting a bin is irreversible! Are you sure you want to delete: " + binNames + "?");

            ButtonType buttonDelete = new ButtonType("Delete");
            ButtonType buttonCancel = new ButtonType("Cancel");
            alert.getButtonTypes().setAll(buttonDelete, buttonCancel);

            if (alert.showAndWait().orElse(buttonCancel) == buttonCancel) {
                return false;
            }
        }

        BinRegistry registry = app.getBinRegistry();
        for (ReplayBin bin : new ArrayList<>(bins)) {
            this.selectionModel.clearSelection(bin);
            this.hidden.remove(bin); // Just in case it's hidden
            registry.deleteBin(bin);

            if (displayed == bin) {
                this.clearDisplay();
            }
        }

        if (displayed == null) {
            this.display(selectionModel.isEmpty() ? BinRegistry.GLOBAL_BIN : selectionModel.getSelectedItems().get(0));
        }

        return true;
    }

    public boolean hideBin(ReplayBin bin) {
        if (bin.isGlobalBin()) {
            return false;
        }

        if (!hidden.add(bin)) {
            return false;
        }

        this.selectionModel.clearSelection(bin);
        if (displayed == bin) {
            this.display(selectionModel.isEmpty() ? BinRegistry.GLOBAL_BIN : selectionModel.getSelectedItems().get(0));
        }

        bin.setHidden(true);
        this.listed.remove(bin.getDisplay());
        return true;
    }

    public void unhide(ReplayBin bin) {
        if (bin.isGlobalBin() || !hidden.contains(bin)) {
            return;
        }

        bin.setHidden(false);
        this.listed.add(bin.getDisplay());
        this.hidden.remove(bin);
    }

    public boolean isHidden(ReplayBin bin) {
        return hidden.contains(bin);
    }

    public Collection<ReplayBin> getHidden() {
        return Collections.unmodifiableCollection(hidden);
    }

    public BinSelectionModel getSelectionModel() {
        return selectionModel;
    }

    private <T> T getAtIndex(Collection<T> collection, int index) {
        if (index < 0 || index >= collection.size()) {
            throw new IllegalArgumentException("Index out of bounds");
        }

        int current = 0;
        for (T element : collection) {
            if (current++ == index) {
                return element;
            }
        }

        return null;
    }

}
