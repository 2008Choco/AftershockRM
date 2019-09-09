package wtf.choco.aftershock.controller;

import wtf.choco.aftershock.App;
import wtf.choco.aftershock.manager.BinRegistry;
import wtf.choco.aftershock.structure.BinDisplayComponent;
import wtf.choco.aftershock.structure.ReplayBin;

import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.layout.TilePane;

public class BinEditorController {

    @FXML private TilePane viewPane;

    private final Image binGraphic = new Image(App.class.getResourceAsStream("/icons/folder.png"));

    @FXML
    public void initialize() {
        App app = App.getInstance();
        BinRegistry binRegistry = app.getBinRegistry();

        ObservableList<Node> viewChildren = viewPane.getChildren();
        for (ReplayBin bin : binRegistry.getBins()) {
            viewChildren.add(new BinDisplayComponent(app, bin, binGraphic));
        }

        binRegistry.getObservableBins().addListener((MapChangeListener<String, ReplayBin>) change -> {
            if (change.wasAdded()) {
                viewChildren.add(new BinDisplayComponent(app, change.getValueAdded(), binGraphic));
            } else {
                viewChildren.removeIf(n -> (n instanceof BinDisplayComponent) && ((BinDisplayComponent) n).getBin() == change.getValueRemoved());
            }
        });
    }

    public BinDisplayComponent getDisplayComponent(ReplayBin bin) {
        for (Node node : viewPane.getChildren()) {
            if (!(node instanceof BinDisplayComponent)) {
                continue;
            }

            BinDisplayComponent binDisplay = (BinDisplayComponent) node;
            if (binDisplay.getBin() == bin) {
                return binDisplay;
            }
        }

        return null;
    }

}
