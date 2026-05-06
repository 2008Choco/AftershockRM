package wtf.choco.aftershock.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import wtf.choco.aftershock.App;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ResourceBundle;

public final class AboutController {

    @FXML private Label headerLabel;
    @FXML private Label builtOnLabel;
    @FXML private Label runtimeLabel;
    @FXML private Label javafxRuntimeLabel;
    @FXML private Label licenseLabel;

    @FXML private ResourceBundle resources;

    @FXML
    private void initialize() {
        this.headerLabel.setText(App.getAppName() + " v" + App.getAppVersion());
        this.builtOnLabel.setText(resources.getString("ui.about.built_on").formatted(App.getAppBuildDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))));
        this.runtimeLabel.setText(resources.getString("ui.about.runtime").formatted(System.getProperty("java.runtime.version")));
        this.javafxRuntimeLabel.setText(resources.getString("ui.about.javafx_runtime").formatted(System.getProperty("javafx.runtime.version")));
        this.licenseLabel.setText(resources.getString("ui.about.license").formatted("MIT"));
    }

    @FXML
    private void onClose() {
        App.getInstance().closeAboutStage();
    }

}
