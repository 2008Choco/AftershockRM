package wtf.choco.aftershock.structure;

import wtf.choco.aftershock.App;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

public class BinDisplayComponent extends VBox {

    private final ReplayBin bin;

    private final ImageView graphic;
    private final Label label;

    {
        this.setAlignment(Pos.TOP_CENTER);
        this.setMaxHeight(55.0);
        this.setPrefWidth(70.0);
        this.setSpacing(5.0);
        this.setPadding(new Insets(5.0, 5.0, 5.0, 5.0));

        this.getStyleClass().add("bin-display");
    }

    public BinDisplayComponent(App app, ReplayBin bin, Image graphic) {
        this.bin = bin;
        this.graphic = new ImageView(graphic);
        this.label = new Label(bin.getName());

        this.label.setTextAlignment(TextAlignment.CENTER);
        this.label.setWrapText(true);

        this.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                app.getController().displayBin(bin);
            }
        });
        this.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> setCursor(Cursor.HAND));
        this.addEventFilter(MouseEvent.MOUSE_EXITED, e -> setCursor(Cursor.DEFAULT));

        this.getChildren().addAll(this.graphic, label);
    }

    public ReplayBin getBin() {
        return bin;
    }

}
