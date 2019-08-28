package wtf.choco.aftershock;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.ConsoleHandler;
import java.util.logging.Logger;

import com.google.gson.Gson;

import wtf.choco.aftershock.controller.AppController;
import wtf.choco.aftershock.manager.ReplayManager;
import wtf.choco.aftershock.replay.ReplayModifiable;
import wtf.choco.aftershock.util.ColouredLogFormatter;
import wtf.choco.aftershock.util.FXUtils;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

public final class App extends Application {

    // https://www.flaticon.com
    private static final Gson GSON = new Gson();
    private static App instance;

    private Stage stage;
    private Scene scene;
    private AppController controller;
    private ResourceBundle resources;

    private Stage settingsStage = null;

    private final ReplayManager replayManager = new ReplayManager();
    private final File installDirectory = new File(System.getProperty("user.home"), "AppData/Roaming/AftershockRM/"); // Temporarily hard-coded

    // TODO: Editable through the application itself
    private final ApplicationSettings settings = new ApplicationSettings(
        "D:/hawke/Documents/My Games/Rocket League/TAGame/Demos/", // Replay directory
        "C:/Users/hawke/AppData/Roaming/AftershockRM/Rattletrap/rattletrap.exe", // Rattletrap directory
        "en_US" // Language
    );

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Logger logger = Logger.getLogger("AftershockRM");

    @Override
    public void init() throws Exception {
        instance = this;
        Locale.setDefault(new Locale("en", "US"));

        this.logger.setUseParentHandlers(false);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(ColouredLogFormatter.get());
        this.logger.addHandler(handler);
    }

    @Override
    public void start(Stage stage) throws IOException {
        this.stage = stage;
        var root = FXUtils.<Parent, AppController>loadFXML("/layout/Root", resources = ResourceBundle.getBundle("/lang/"));
        this.scene = new Scene(root.getKey());
        this.controller = root.getValue();

        // Stage setup
        stage.setTitle("Aftershock Replay Manager");
        stage.setScene(scene);
        stage.show();

        this.replayManager.attachTable(controller.getReplayTable());

        // Replay setup
        this.installDirectory.mkdirs();
        File replayDirectory = new File(settings.getReplayLocation());

        File replayCacheDirectory = new File(installDirectory, "Cache");
        replayCacheDirectory.mkdir();
        File replayHeadersDirectory = new File(installDirectory, "Headers");
        replayHeadersDirectory.mkdir();

        if (replayDirectory.exists() && replayDirectory.isDirectory()) {
            this.logger.info("Running startup replay caching processes...");
            long now = System.currentTimeMillis();

            this.executor.execute(() -> {
                for (File replayFile : replayDirectory.listFiles((f, name) -> name.endsWith(".replay"))) {
                    String replayFileName = replayFile.getName();

                    // Backup the replay file for later
                    File cacheDestination = new File(replayCacheDirectory, replayFileName);
                    if (!cacheDestination.exists()) {
                        this.logger.info("Copying replay with ID: " + replayFileName);
                        try {
                            Files.copy(replayFile.toPath(), cacheDestination.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    File headerDestination = new File(replayHeadersDirectory, replayFileName.substring(0, replayFileName.lastIndexOf('.')) + ".json");
                    ReplayModifiable replay = new ReplayModifiable(cacheDestination, headerDestination);

                    if (headerDestination.exists()) {
                        replay.loadDataFromFile(GSON);
                        this.replayManager.addReplay(replay);
                        continue;
                    }

                    this.logger.info("Creating header file for replay with ID: " + replayFileName);

                    try {
                        Runtime.getRuntime().exec(settings.getRattletrapPath() + " --f --i \"" + replayFile.getAbsolutePath() + "\" --o \"" + headerDestination.getAbsolutePath() + "\"").waitFor();
                        replay.loadDataFromFile(GSON);
                        this.replayManager.addReplay(replay);
                        this.logger.info("Done!");
                    } catch (IOException | InterruptedException e) {
                        this.logger.severe("Failed exceptionally:");
                        e.printStackTrace();
                    }
                }

                long time = System.currentTimeMillis() - now;
                this.logger.info("Loaded " + replayManager.getReplayCount() + " replays in " + time + " milliseconds!");
                this.controller.requestLabelUpdate();
            });
        } else {
            this.logger.warning("Could not find replay directory at path " + settings.getReplayLocation());
        }
    }

    @Override
    public void stop() throws Exception {
        this.executor.shutdown();
        this.replayManager.clearReplays();
    }

    public Logger getLogger() {
        return logger;
    }

    public Stage getStage() {
        return stage;
    }

    public ResourceBundle getResources() {
        return resources;
    }

    public Stage openSettingsStage() {
        if (settingsStage != null) {
            return settingsStage;
        }

        Parent root = FXUtils.loadFXMLRoot("/layout/SettingsPanel", resources);
        if (root == null) {
            return null;
        }

        Scene scene = new Scene(root);
        Stage stage = new Stage();
        stage.setTitle("Aftershock Replay Manager - Application Settings");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setResizable(false);

        stage.setScene(scene);
        stage.show();

        this.settingsStage = stage;
        return stage;
    }

    public void closeSettingsStage() {
        this.settingsStage.close();
        this.settingsStage = null;
    }

    public ApplicationSettings getSettings() {
        return settings;
    }

    public File getInstallDirectory() {
        return installDirectory;
    }

    public static App getInstance() {
        return instance;
    }

    public static void main(String[] args) {
        launch(args);
    }

}