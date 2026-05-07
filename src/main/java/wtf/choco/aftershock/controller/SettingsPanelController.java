package wtf.choco.aftershock.controller;

import javafx.application.Platform;
import javafx.beans.property.Property;
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
import wtf.choco.aftershock.settings.ApplicationSettings;
import wtf.choco.aftershock.settings.BooleanSetting;
import wtf.choco.aftershock.settings.PathSetting;
import wtf.choco.aftershock.settings.ReplayDateFormat;
import wtf.choco.aftershock.settings.Setting;
import wtf.choco.aftershock.structure.ReplayBin;
import wtf.choco.aftershock.util.TranslatableStringConverter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Level;

public final class SettingsPanelController {

    @FXML private TextField fieldReplayFolder;
    @FXML private TextField fieldReplayEditorPath;
    @FXML private ComboBox<ReplayDateFormat> comboBoxDateFormat;
    @FXML private CheckBox checkbox24HourTime;
    @FXML private ComboBox<Locale> comboBoxLanguage;

    @FXML
    private void initialize() {
        this.fieldReplayFolder.setText(ApplicationSettings.REPLAY_DIRECTORY.serializeCurrentValue());

        this.fieldReplayEditorPath.setText(ApplicationSettings.REPLAY_EDITOR_PATH.serializeCurrentValue());

        this.comboBoxDateFormat.setValue(ApplicationSettings.REPLAY_DATE_FORMAT.getValue());
        this.comboBoxDateFormat.setItems(FXCollections.observableArrayList(ReplayDateFormat.values()));
        this.comboBoxDateFormat.setConverter(TranslatableStringConverter.get());

        this.checkbox24HourTime.setSelected(ApplicationSettings.REPLAY_24_HOUR_TIME.get());

        this.comboBoxLanguage.setValue(ApplicationSettings.LOCALE.getValue());
        this.comboBoxLanguage.getItems().add(Locale.of("en_US")); // TODO: Temporarily hard-coded. Should pull from available lang files
        this.comboBoxLanguage.setConverter(ApplicationSettings.LOCALE.converter());
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
        boolean replayDirectoryChanged = updateSetting(ApplicationSettings.REPLAY_DIRECTORY, fieldReplayFolder.getText());
        this.updateSetting(ApplicationSettings.REPLAY_EDITOR_PATH, fieldReplayEditorPath.getText());
        this.updateSetting(ApplicationSettings.REPLAY_DATE_FORMAT, comboBoxDateFormat.getValue());
        this.updateSetting(ApplicationSettings.REPLAY_24_HOUR_TIME, checkbox24HourTime.isSelected());
        this.updateSetting(ApplicationSettings.LOCALE, comboBoxLanguage.getValue());

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
                    App.LOGGER.log(Level.SEVERE, "Failed to reload live replay directory after settings change! (new path: \"" + ApplicationSettings.REPLAY_DIRECTORY.getValue() + "\")", e);
                    return null;
                });
        }

        App.getInstance().closeSettingsStage();
    }

    private <T, V extends Property<T>> boolean updateSetting(Setting<T, V> setting, T value) {
        T before = setting.getValue();
        if (Objects.equals(before, value)) {
            return false;
        }

        setting.setValue(value);
        return true;
    }

    private boolean updateSetting(BooleanSetting setting, boolean value) {
        if (value == setting.get()) {
            return false;
        }

        setting.set(value);
        return true;
    }

    private boolean updateSetting(PathSetting setting, String stringifiedPath) {
        if (stringifiedPath == null || stringifiedPath.isBlank()) {
            return updateSetting(setting, (Path) null);
        }

        return updateSetting(setting, Path.of(stringifiedPath));
    }

}
