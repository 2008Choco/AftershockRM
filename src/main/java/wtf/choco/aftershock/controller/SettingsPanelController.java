package wtf.choco.aftershock.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import wtf.choco.aftershock.App;
import wtf.choco.aftershock.ApplicationSettings;
import wtf.choco.aftershock.ApplicationSettings.Setting;
import wtf.choco.aftershock.settings.ReplayDateFormat;
import wtf.choco.aftershock.structure.ReplayBin;
import wtf.choco.aftershock.util.TranslatableStringConverter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Level;

public final class SettingsPanelController {

    @FXML private TextField fieldReplayFolder;
    @FXML private TextField fieldReplayEditorPath;
    @FXML private ComboBox<ReplayDateFormat> comboBoxDateFormat;
    @FXML private CheckBox checkbox24HourTime;
    @FXML private ComboBox<String> comboBoxLanguage;

    @FXML
    private void initialize() {
        this.fieldReplayFolder.setText(ApplicationSettings.REPLAY_DIRECTORY.get());
        this.fieldReplayEditorPath.setText(ApplicationSettings.REPLAY_EDITOR_PATH.get());
        this.comboBoxDateFormat.setValue(Enum.valueOf(ReplayDateFormat.class, ApplicationSettings.REPLAY_DATE_FORMAT.get()));
        this.comboBoxDateFormat.setItems(FXCollections.observableArrayList(ReplayDateFormat.values()));
        this.comboBoxDateFormat.setConverter(TranslatableStringConverter.get());
        this.checkbox24HourTime.setSelected(Boolean.parseBoolean(ApplicationSettings.REPLAY_24_HOUR_TIME.get()));
        this.comboBoxLanguage.setValue(ApplicationSettings.LOCALE.get());
        this.comboBoxLanguage.getItems().add("en_US"); // TODO: Temporarily hard-coded. Should pull from available lang files
    }

    @FXML
    private void onClickSelectReplayDirectory() {
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
    private void onClickSelectReplayEditorFile() {
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
    private void onClickCloseWithoutSaving() {
        App.getInstance().closeSettingsStage();
    }

    @FXML
    private void onClickApplyAndClose() {
        boolean replayDirectoryChanged = setIfValid(ApplicationSettings.REPLAY_DIRECTORY, fieldReplayFolder.getText());
        this.setIfValid(ApplicationSettings.REPLAY_EDITOR_PATH, fieldReplayEditorPath.getText());
        this.setIfValid(ApplicationSettings.REPLAY_DATE_FORMAT, comboBoxDateFormat.getValue().name());
        this.setIfValid(ApplicationSettings.REPLAY_24_HOUR_TIME, Boolean.toString(checkbox24HourTime.isSelected()));
        this.setIfValid(ApplicationSettings.LOCALE, comboBoxLanguage.getValue());

        App app = App.getInstance();
        app.getExecutor().execute(() -> {
            try {
                ApplicationSettings.save(app.getFileStructure());
            } catch (IOException e) {
                App.LOGGER.log(Level.SEVERE, "Failed to save application settings!", e);
            }
        });
        if (replayDirectoryChanged) {
            app.getBinRegistry().clearBins(true);
            app.getFileOperations().loadReplays()
                .thenAcceptAsync(ReplayBin.GLOBAL.getReplays()::addAll, Platform::runLater)
                .exceptionally(e -> {
                    App.LOGGER.log(Level.SEVERE, "Failed to reload live replay directory after settings change! (new path: \"" + ApplicationSettings.REPLAY_DIRECTORY.get() + "\")", e);
                    return null;
                });
        }

        App.LOGGER.info("Settings updated to: ");
        App.LOGGER.info("Replay Directory: " + ApplicationSettings.REPLAY_DIRECTORY.get());
        App.LOGGER.info("Replay Editor Path: " + ApplicationSettings.REPLAY_EDITOR_PATH.get());
        App.LOGGER.info("Replay DateFormat: " + ApplicationSettings.REPLAY_DATE_FORMAT.get());
        App.LOGGER.info("Replay 24 Hour Time: " + ApplicationSettings.REPLAY_24_HOUR_TIME.get());
        App.LOGGER.info("Language: " + ApplicationSettings.LOCALE.get());

        App.getInstance().closeSettingsStage();
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
