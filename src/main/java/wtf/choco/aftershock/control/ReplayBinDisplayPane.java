package wtf.choco.aftershock.control;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableIntegerValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import wtf.choco.aftershock.App;
import wtf.choco.aftershock.manager.BinRegistry;
import wtf.choco.aftershock.structure.ReplayBin;
import wtf.choco.aftershock.util.ComplexBindings;
import wtf.choco.aftershock.util.FXUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class ReplayBinDisplayPane extends VBox {

    @FXML private Label labelHiddenBins;
    @FXML private VBox binEditorList;

    @FXML private MenuItem menuItemUnhideBins;

    @FXML private ResourceBundle resources;

    private final ReplayBinSelectionModel selectionModel = new ReplayBinSelectionModel();
    private final Map<ReplayBin, Node> replayBinDisplayNodes = new HashMap<>();

    private final ListProperty<ReplayBin> replayBins  = new SimpleListProperty<>(this, "replayBins", FXCollections.observableArrayList());
    private final ObjectProperty<ReplayBin> activeBin = new SimpleObjectProperty<>(this, "activeBin");
    private final ObservableIntegerValue hiddenBinCount = ComplexBindings.createIntegerBindingCountingBooleanProperties(replayBins, ReplayBin::hiddenProperty);

    public ReplayBinDisplayPane() {
        FXUtils.loadFXMLComponent("/component/ReplayBinDisplayPane", this, App.getInstance().getResources());
    }

    //<editor-fold desc="Object property methods">
    public ObservableList<ReplayBin> getReplayBins() {
        return replayBinsProperty().get();
    }

    public ListProperty<ReplayBin> replayBinsProperty() {
        return replayBins;
    }

    public void setActiveBin(ReplayBin bin) {
        this.activeBinProperty().set(bin);
    }

    public ReplayBin getActiveBin() {
        return activeBinProperty().get();
    }

    public ObjectProperty<ReplayBin> activeBinProperty() {
        return activeBin;
    }
    //</editor-fold>

    @FXML
    private void initialize() {
        this.replayBinsProperty().addListener((ListChangeListener.Change<? extends ReplayBin> change) -> {
            while (change.next()) {
                for (ReplayBin bin : change.getAddedSubList()) {
                    ReplayBinDisplay binDisplay = createReplayBinDisplay(bin);
                    this.replayBinDisplayNodes.put(bin, binDisplay);

                    if (!bin.isHidden()) {
                        this.binEditorList.getChildren().add(binDisplay);
                    }
                }

                for (ReplayBin bin : change.getRemoved()) {
                    Node node = replayBinDisplayNodes.remove(bin);
                    if (node != null) {
                        this.binEditorList.getChildren().remove(node);
                    }

                    if (bin == getActiveBin()) {
                        this.setActiveBin(App.getInstance().getBinRegistry().getGlobalBin());
                    }
                }
            }
        });
        this.replayBinsProperty().bindBidirectional(App.getInstance().getBinRegistry().binsProperty());

        this.activeBinProperty().addListener((_, oldValue, newValue) -> {
            if (oldValue != null) {
                oldValue.setActive(false);
            }

            if (newValue != null) {
                newValue.setActive(true);
            }
        });

        // TODO: This needs to be done better, with a Pseudoclass, probably. Figure out how to use these
        this.selectionModel.getSelectedItems().addListener((ListChangeListener<? super ReplayBin>) change -> {
            while (change.next()) {
                change.getAddedSubList().forEach(bin -> replayBinDisplayNodes.get(bin).getStyleClass().add("replay-bin-display-selected"));
                change.getRemoved().forEach(bin -> replayBinDisplayNodes.get(bin).getStyleClass().remove("replay-bin-display-selected"));
            }
        });

        this.labelHiddenBins.visibleProperty().bind(Bindings.greaterThan(hiddenBinCount, 0));
        this.labelHiddenBins.textProperty().bind(hiddenBinCount.map(value -> resources.getString("ui.bin_editor.hidden_bins").formatted(value)));

        this.menuItemUnhideBins.disableProperty().bind(Bindings.equal(hiddenBinCount, 0));
    }

    private ReplayBinDisplay createReplayBinDisplay(ReplayBin bin) {
        ReplayBinDisplay display = new ReplayBinDisplay();

        // Display properties
        display.setName(bin.getName());
        display.nameProperty().bindBidirectional(bin.nameProperty());
        display.mutableProperty().bind(bin.globalProperty().not());
        display.replaysProperty().bindBidirectional(bin.replaysProperty());
        display.activeProperty().bindBidirectional(bin.activeProperty());
        display.setOnMouseClicked(event -> onReplayBinDisplayClicked(event, bin));
        display.setOnDelete(_ -> deleteBin(bin));
        display.setOnHide(_ -> bin.setHidden(true));
        display.setOnClone(_ -> App.getInstance().getBinRegistry().addBin(new ReplayBin(bin)));

        // Bin properties
        bin.hiddenProperty().addListener((_, _, newValue) -> {
            Node node = replayBinDisplayNodes.get(bin);

            if (newValue) {
                this.selectionModel.clearSelection(bin);
                if (getActiveBin() == bin) {
                    this.setActiveBin(App.getInstance().getBinRegistry().getGlobalBin());
                }

                if (node != null) {
                    this.binEditorList.getChildren().remove(node);
                }
            } else if (node != null) {
                int index = replayBins.indexOf(bin);
                this.binEditorList.getChildren().add(index, node);
            }
        });

        return display;
    }

    private void onReplayBinDisplayClicked(MouseEvent event, ReplayBin bin) {
        if (event.isControlDown()) {
            if (!selectionModel.isSelected(bin)) {
                this.selectionModel.select(bin);
                if (getActiveBin() == null) {
                    this.setActiveBin(bin);
                }
            } else {
                this.selectionModel.clearSelection(bin);
            }
        } else {
            if (event.isShiftDown()) {
                ObservableList<Integer> selectedIndices = selectionModel.getSelectedIndices();
                if (!selectedIndices.isEmpty()) {
                    int mostRecentIndex = selectedIndices.getLast();
                    selectionModel.clearSelection();
                    selectionModel.selectRange(replayBins.indexOf(bin), mostRecentIndex);
                    selectionModel.select(replayBins.get(mostRecentIndex));
                }
            } else if (event.getButton() == MouseButton.PRIMARY) {
                this.setActiveBin(bin);
                this.selectionModel.clearAndSelect(bin);
            }
        }
    }

    @FXML
    private void createBin(ActionEvent event) {
        int count = 0;
        String binName = "New Bin";

        BinRegistry binRegistry = App.getInstance().getBinRegistry();
        ReplayBin bin = null;
        while ((bin = binRegistry.createBin(binName + (count++ >= 1 ? " (" + count + ")" : ""))) == null);

        this.setActiveBin(bin);
        this.selectionModel.clearAndSelect(bin);
    }

    @FXML
    private void deleteBin(ActionEvent event) {
        this.deleteMultipleBins(selectionModel.getSelectedItems());
    }

    @FXML
    private void onUnhideBins(ActionEvent event) {
        App.getInstance().getBinRegistry().getBins().forEach(bin -> {
            if (bin.isHidden()) {
                bin.setHidden(false);
            }
        });
    }

    private void deleteBin(ReplayBin bin) {
        if (bin.isGlobal()) {
            java.awt.Toolkit.getDefaultToolkit().beep();
            return;
        }

        if (!bin.isEmpty()) { // If the bin isn't empty, let's request confirmation
            if (!promptToConfirm("single", bin.getName(), resources)) {
                return;
            }
        }

        this.actuallyDeleteBin(bin);
    }

    private void deleteMultipleBins(List<ReplayBin> bins) {
        bins = new ArrayList<>(bins); // Make a copy because we're going to mutate this "bins" list
        bins.removeIf(ReplayBin::isGlobal); // Never delete the global bin

        // Fast path for one bin in the list
        if (bins.size() == 1) {
            this.deleteBin(bins.getFirst());
            return;
        }

        boolean allEmpty = bins.stream().allMatch(ReplayBin::isEmpty);
        if (!allEmpty) { // If there's at least one bin that isn't empty, request confirmation
            String binNames = bins.stream().map(b -> '"' + b.getName() + '"').collect(Collectors.joining(", "));
            if (!promptToConfirm("multiple", binNames, resources)) {
                return;
            }
        }

        bins.forEach(this::actuallyDeleteBin);
    }

    private boolean promptToConfirm(String singleOrMultiple, String binName, ResourceBundle resources) {
        Alert confirmation = new Alert(Alert.AlertType.WARNING);
        confirmation.setTitle(resources.getString("ui.bin_editor.delete.confirm." + singleOrMultiple + ".title"));
        confirmation.setHeaderText(resources.getString("ui.bin_editor.delete.confirm." + singleOrMultiple + ".header"));
        confirmation.setContentText(resources.getString("ui.bin_editor.delete.confirm." + singleOrMultiple + ".content").formatted(binName));

        ButtonType buttonDelete = new ButtonType(resources.getString("ui.bin_editor.delete.confirm." + singleOrMultiple + ".delete"));
        ButtonType buttonCancel = new ButtonType(resources.getString("ui.bin_editor.delete.confirm." + singleOrMultiple + ".cancel"));
        confirmation.getButtonTypes().setAll(buttonDelete, buttonCancel);

        return confirmation.showAndWait().orElse(buttonCancel) != buttonCancel;
    }

    private void actuallyDeleteBin(ReplayBin bin) {
        this.selectionModel.clearSelection(bin);
        bin.setHidden(false);

        if (getActiveBin() == bin) {
            ReplayBin globalBin = App.getInstance().getBinRegistry().getGlobalBin();
            this.setActiveBin(globalBin);
            this.selectionModel.clearAndSelect(globalBin);
        }

        App.getInstance().getBinRegistry().deleteBin(bin);
    }

    public ReplayBinSelectionModel getSelectionModel() {
        return selectionModel;
    }

    public final class ReplayBinSelectionModel extends MultipleSelectionModel<ReplayBin> {

        private final ObservableList<Integer> selectedIndices = FXCollections.observableArrayList();
        private final ObservableList<ReplayBin> selected = FXCollections.observableArrayList();

        private ReplayBinSelectionModel() { }

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
            for (int i : indices) {
                this.select(i);
            }
        }

        @Override
        public void selectAll() {
            this.selected.setAll(replayBins);
            this.selectedIndices.setAll(IntStream.range(0, replayBins.size()).boxed().toList());
        }

        @Override
        public void selectFirst() {
            this.select(replayBins.getFirst());
        }

        @Override
        public void selectLast() {
            this.select(replayBins.getLast());
        }

        @Override
        public void clearAndSelect(int index) {
            if (index < 0 || index >= replayBins.size()) {
                return;
            }

            this.selected.clear();
            this.selectedIndices.clear();
            this.select(index);
        }

        public void clearAndSelect(ReplayBin bin) {
            this.clearAndSelect(replayBins.indexOf(bin));
        }

        @Override
        public void select(int index) {
            if (index < 0 || index >= replayBins.size()) {
                return;
            }

            ReplayBin bin = replayBins.get(index);
            if (!selected.contains(bin)) {
                selected.add(bin);
            }

            if (!selectedIndices.contains(index)) {
                this.selectedIndices.add(index);
            }
        }

        @Override
        public void select(ReplayBin replayBin) {
            this.select(replayBins.indexOf(replayBin));
        }

        @Override
        public void clearSelection(int index) {
            if (index < 0 || index >= replayBins.size()) {
                return;
            }

            this.selected.remove(replayBins.get(index));
            this.selectedIndices.remove((Integer) index);
        }

        public void clearSelection(ReplayBin replayBin) {
            this.clearSelection(replayBins.indexOf(replayBin));
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

}
