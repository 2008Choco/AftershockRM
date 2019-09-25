package wtf.choco.aftershock.controller;

import java.io.File;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.logging.Logger;

import wtf.choco.aftershock.App;
import wtf.choco.aftershock.ApplicationSettings;
import wtf.choco.aftershock.ApplicationSettings.Setting;

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

        this.fieldReplayFolder.setText(settings.get(ApplicationSettings.REPLAY_DIRECTORY));
        this.fieldRattletrapPath.setText(settings.get(ApplicationSettings.RATTLETRAP_PATH));
        this.languageSelector.setValue(settings.get(ApplicationSettings.LOCALE));
    }

    @FXML
    @SuppressWarnings("unused")
    public void selectReplayFolder(ActionEvent event) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Replay Directory");

        String replayFolderPath = fieldReplayFolder.getText();
        if (!replayFolderPath.isBlank()) {
            File initialDirectory = (isValidPath(replayFolderPath)) ? new File(replayFolderPath) : App.getInstance().getInstallDirectory();
            chooser.setInitialDirectory(initialDirectory);
        }

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

        String rattletrapPath = fieldRattletrapPath.getText();
        if (!rattletrapPath.isBlank()) {
            File initialDirectory = (isValidPath(rattletrapPath)) ? new File(rattletrapPath).getParentFile() : App.getInstance().getInstallDirectory();
            chooser.setInitialDirectory(initialDirectory);
        }

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

        boolean replayDirectoryChanged = setIfValid(settings, ApplicationSettings.REPLAY_DIRECTORY, fieldReplayFolder.getText());
        this.setIfValid(settings, ApplicationSettings.RATTLETRAP_PATH, fieldRattletrapPath.getText());
        this.setIfValid(settings, ApplicationSettings.LOCALE, languageSelector.getValue());

        App app = App.getInstance();
        app.getExecutor().execute(settings::writeToFile);
        if (replayDirectoryChanged) {
            app.reloadReplays(null);
        }

        Logger logger = App.getInstance().getLogger();
        logger.info("Settings updated to: ");
        logger.info("Replay Directory: " + settings.get(ApplicationSettings.REPLAY_DIRECTORY));
        logger.info("Rattletrap Path: " + settings.get(ApplicationSettings.RATTLETRAP_PATH));
        logger.info("Language: " + settings.get(ApplicationSettings.LOCALE));

        this.close(event);
    }

    private boolean setIfValid(ApplicationSettings settings, Setting key, String value) {
        if (value == null || (value = value.trim()).isEmpty()) {
            return false;
        }

        String before = settings.get(key);
        settings.set(key, value);
        return !before.equals(value);
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
