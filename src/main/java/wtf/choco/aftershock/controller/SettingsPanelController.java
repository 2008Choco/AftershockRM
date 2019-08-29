package wtf.choco.aftershock.controller;

import java.io.File;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.function.Consumer;
import java.util.logging.Logger;

import wtf.choco.aftershock.App;
import wtf.choco.aftershock.ApplicationSettings;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

public final class SettingsPanelController {

    @FXML private TextField fieldReplayFolder, fieldRattletrapPath;
    @FXML private ComboBox<String> languageSelector;

    @FXML
    public void initialize() {
        ApplicationSettings settings = App.getInstance().getSettings();

        this.fieldReplayFolder.setText(settings.getReplayLocation());
        this.fieldRattletrapPath.setText(settings.getRattletrapPath());
        this.languageSelector.setValue(settings.getLocale());
    }

    @FXML
    @SuppressWarnings("unused")
    public void selectReplayFolder(ActionEvent event) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Replay Directory");

        File initialDirectory = (isValidPath(fieldReplayFolder.getText())) ? new File(fieldReplayFolder.getText()) : App.getInstance().getInstallDirectory();
        chooser.setInitialDirectory(initialDirectory);

        File directory = chooser.showDialog(new Stage());
        if (directory != null) {
            this.fieldReplayFolder.setText(directory.getAbsolutePath());
        }
    }

    @FXML
    @SuppressWarnings("unused")
    public void selectRattletrapFile(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Rattletrap Executable");
        chooser.setSelectedExtensionFilter(new ExtensionFilter("Executable File", "*.exe"));

        File initialDirectory = (isValidPath(fieldRattletrapPath.getText())) ? new File(fieldRattletrapPath.getText()).getParentFile() : App.getInstance().getInstallDirectory();
        chooser.setInitialDirectory(initialDirectory);

        File file = chooser.showOpenDialog(new Stage());
        if (file != null) {
            this.fieldRattletrapPath.setText(file.getAbsolutePath());
        }
    }

    @FXML
    @SuppressWarnings("unused")
    public void close(ActionEvent event) {
        App.getInstance().closeSettingsStage();
    }

    @FXML
    public void applyAndClose(ActionEvent event) {
        ApplicationSettings settings = App.getInstance().getSettings();

        this.setIfValid(fieldReplayFolder.getText(), settings::setReplayLocation);
        this.setIfValid(fieldRattletrapPath.getText(), settings::setRattletrapPath);
        this.setIfValid(languageSelector.getValue(), settings::setLocale);

        Logger logger = App.getInstance().getLogger();
        logger.info("Settings updated to: ");
        logger.info("Replay Directory: " + settings.getReplayLocation());
        logger.info("Rattletrap Path: " + settings.getRattletrapPath());
        logger.info("Language: " + settings.getLocale());

        this.close(event);
    }

    private void setIfValid(String value, Consumer<String> applyFunction) {
        if (value == null || (value = value.trim()).isEmpty()) {
            return;
        }

        applyFunction.accept(value);
    }

    // Doesn't work on Linux... for some reason... always true
    public static boolean isValidPath(String path) {
        try {
            Paths.get(path);
        } catch (InvalidPathException | NullPointerException ex) {
            return false;
        }

        return true;
    }

}
