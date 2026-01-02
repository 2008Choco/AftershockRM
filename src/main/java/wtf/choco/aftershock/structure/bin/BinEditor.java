package wtf.choco.aftershock.structure.bin;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
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
import wtf.choco.aftershock.App;
import wtf.choco.aftershock.controller.AppController;
import wtf.choco.aftershock.manager.BinRegistry;
import wtf.choco.aftershock.structure.ReplayBin;
import wtf.choco.aftershock.structure.ReplayEntry;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class BinEditor {

    private final ObjectProperty<ReplayBin> displayed;

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
        binRegistry.getBins().forEach(bin -> listed.add(bin.getDisplay()));
        this.displayed = new SimpleObjectProperty<>(binRegistry.getGlobalBin());

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

        this.hidden.addListener((SetChangeListener<ReplayBin>) change -> {
            ObservableList<Node> children = node.getChildren();

            int newSize = change.getSet().size();
            if (newSize == 0) {
                children.remove(1);
                return;
            }

            if (children.size() == 2) {
                children.add(1, hiddenLabel);
            }

            this.hiddenLabel.setText(app.getResources().getString("ui.bin_editor.hidden_bins").formatted(newSize));
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
        this.displayed.set(bin);

        if (bin == null) {
            this.replayTable.setItems(FXCollections.emptyObservableList());
            return;
        }

        ObservableList<ReplayEntry> entries = (controller.getTableFilter().isInvalid()) ? bin.getReplaysObservable() : new FilteredList<>(bin.getReplaysObservable(), controller.getTableFilter());
        this.replayTable.setItems(entries);
        this.selectionModel.select(bin);
    }

    public void clearDisplay() {
        this.display(app.getBinRegistry().getGlobalBin());
    }

    public ReplayBin getDisplayed() {
        return displayed.get();
    }

    public ReadOnlyObjectProperty<ReplayBin> displayedProperty() {
        return displayed;
    }

    public boolean deleteBin(ReplayBin bin, boolean shouldAlert) {
        if (bin == null) {
            return false;
        }

        if (bin.isGlobalBin()) {
            if (shouldAlert) {
                Toolkit.getDefaultToolkit().beep();
            }

            return false;
        }

        if (!bin.isEmpty() && shouldAlert) {
            ResourceBundle resources = app.getResources();

            Alert alert = new Alert(AlertType.WARNING);
            alert.setTitle(resources.getString("ui.bin_editor.delete.confirm.single.title"));
            alert.setHeaderText(resources.getString("ui.bin_editor.delete.confirm.single.header"));
            alert.setContentText(resources.getString("ui.bin_editor.delete.confirm.single.content").formatted(bin.getName()));

            ButtonType buttonDelete = new ButtonType(resources.getString("ui.bin_editor.delete.confirm.single.delete"));
            ButtonType buttonCancel = new ButtonType(resources.getString("ui.bin_editor.delete.confirm.single.cancel"));
            alert.getButtonTypes().setAll(buttonDelete, buttonCancel);

            if (alert.showAndWait().orElse(buttonCancel) == buttonCancel) {
                return false;
            }
        }

        this.selectionModel.clearSelection(bin);
        this.hidden.remove(bin); // Just in case it's hidden
        this.app.getBinRegistry().deleteBin(bin);

        if (displayed.get() == bin) {
            this.display(selectionModel.isEmpty() ? app.getBinRegistry().getGlobalBin() : selectionModel.getSelectedItems().getFirst());
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

        bins.removeIf(ReplayBin::isGlobalBin);
        boolean allEmpty = bins.stream().allMatch(ReplayBin::isEmpty);
        if (!allEmpty && shouldAlert) {
            ResourceBundle resources = app.getResources();
            String binNames = bins.stream().map(b -> '"' + b.getName() + '"').collect(Collectors.joining(", "));

            Alert alert = new Alert(AlertType.WARNING);
            alert.setTitle(resources.getString("ui.bin_editor.delete.confirm.multiple.title"));
            alert.setHeaderText(resources.getString("ui.bin_editor.delete.confirm.multiple.header"));
            alert.setContentText(resources.getString("ui.bin_editor.delete.confirm.multiple.content").formatted(binNames));

            ButtonType buttonDelete = new ButtonType(resources.getString("ui.bin_editor.delete.confirm.multiple.delete"));
            ButtonType buttonCancel = new ButtonType(resources.getString("ui.bin_editor.delete.confirm.multiple.cancel"));
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

            if (displayed.get() == bin) {
                this.clearDisplay();
            }
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
        if (displayed.get() == bin) {
            this.display(selectionModel.isEmpty() ? app.getBinRegistry().getGlobalBin() : selectionModel.getSelectedItems().getFirst());
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
