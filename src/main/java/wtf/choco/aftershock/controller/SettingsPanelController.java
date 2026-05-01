package wtf.choco.aftershock.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import wtf.choco.aftershock.App;
import wtf.choco.aftershock.ApplicationSettings;
import wtf.choco.aftershock.ApplicationSettings.Setting;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Logger;

public final class SettingsPanelController {

    @FXML private TextField fieldReplayFolder, fieldRocketRPPath, fieldReplayEditorPath;
    @FXML private ComboBox<String> languageSelector;

    @FXML
    public void initialize() {
        this.fieldReplayFolder.setText(ApplicationSettings.REPLAY_DIRECTORY.get());
        this.fieldRocketRPPath.setText(ApplicationSettings.ROCKETRP_PATH.get());
        this.fieldReplayEditorPath.setText(ApplicationSettings.REPLAY_EDITOR_PATH.get());
        this.languageSelector.setValue(ApplicationSettings.LOCALE.get());
    }

    @FXML
    @SuppressWarnings("unused")
    public void selectReplayFolder(ActionEvent event) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Replay Directory");

        String replayFolderPath = fieldReplayFolder.getText();
        if (!replayFolderPath.isBlank()) {
            chooser.setInitialDirectory(Path.of(replayFolderPath).toFile());
        }

        File directory = chooser.showDialog(new Stage());
        if (directory != null) {
            this.fieldReplayFolder.setText(directory.getAbsolutePath());
        }
    }

    @FXML
    @SuppressWarnings("unused")
    public void selectRocketRPPath(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select RocketRP Executable");
        chooser.setSelectedExtensionFilter(new ExtensionFilter("Executable File", "exe"));

        String rocketRPPath = fieldRocketRPPath.getText();
        if (!rocketRPPath.isBlank()) {
            chooser.setInitialDirectory(Path.of(rocketRPPath).getParent().toFile());
        }

        File file = chooser.showOpenDialog(new Stage());
        if (file != null) {
            this.fieldRocketRPPath.setText(file.getAbsolutePath());
        }
    }

    @FXML
    @SuppressWarnings("unused")
    public void selectReplayEditorFile(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Replay Editor Executable");
        chooser.setSelectedExtensionFilter(new ExtensionFilter("Executable File", "exe"));

        String replayEditorPath = fieldReplayEditorPath.getText();
        if (!replayEditorPath.isBlank()) {
            chooser.setInitialDirectory(Path.of(replayEditorPath).getParent().toFile());
        }

        File file = chooser.showOpenDialog(new Stage());
        if (file != null) {
            this.fieldReplayEditorPath.setText(file.getAbsolutePath());
        }
    }

    @FXML
    @SuppressWarnings("unused")
    public void close(ActionEvent event) {
        App.getInstance().closeSettingsStage();
    }

    @FXML
    public void applyAndClose(ActionEvent event) {
        boolean replayDirectoryChanged = setIfValid(ApplicationSettings.REPLAY_DIRECTORY, fieldReplayFolder.getText());
        this.setIfValid(ApplicationSettings.ROCKETRP_PATH, fieldRocketRPPath.getText());
        this.setIfValid(ApplicationSettings.REPLAY_EDITOR_PATH, fieldReplayEditorPath.getText());
        this.setIfValid(ApplicationSettings.LOCALE, languageSelector.getValue());

        App app = App.getInstance();
        app.getExecutor().execute(() -> {
            try {
                ApplicationSettings.save(app.getFileStructure());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        if (replayDirectoryChanged) {
            app.getBinRegistry().clearBins(true);
            app.getFileOperations().performCompleteRefresh()
                .thenAcceptAsync(app.getBinRegistry().getGlobalBin().getReplays()::addAll, Platform::runLater)
                .exceptionally(e -> {
                    e.printStackTrace();
                    return null;
                });
        }

        Logger logger = app.getLogger();
        logger.info("Settings updated to: ");
        logger.info("Replay Directory: " + ApplicationSettings.REPLAY_DIRECTORY.get());
        logger.info("RocketRP Path: " + ApplicationSettings.ROCKETRP_PATH.get());
        logger.info("Replay Editor Path: " + ApplicationSettings.REPLAY_EDITOR_PATH.get());
        logger.info("Language: " + ApplicationSettings.LOCALE.get());

        this.close(event);
    }

    private boolean setIfValid(Setting setting, String value) {
        if (value == null) {
            return false;
        }

        String before = setting.get();
        if (before.equals(value)) {
            return false;
        }

        setting.set(value);
        return true;
    }

}
