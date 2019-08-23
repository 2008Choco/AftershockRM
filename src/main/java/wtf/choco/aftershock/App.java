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

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class App extends Application {

    // https://www.flaticon.com
    private static final Gson GSON = new Gson();
    private static App instance;

    private Stage stage;
    private Scene scene;
    private AppController controller;
    private ResourceBundle resources;

    private final ReplayManager replayManager = new ReplayManager();

    // TODO: Editable through the application itself
    private final ApplicationSettings settings = new ApplicationSettings(
        "D:/hawke/Documents/My Games/Rocket League/TAGame/Demos/", // Replay directory
        "C:/Users/hawke/AppData/Roaming/AftershockRM/", // Install directory
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
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("/Root.fxml"), resources = ResourceBundle.getBundle("/lang/"));
        this.scene = new Scene(fxmlLoader.load());
        this.controller = fxmlLoader.getController();

        // Stage setup
        stage.setTitle("Aftershock Replay Manager");
        stage.setScene(scene);
        stage.show();

        this.replayManager.attachTable(controller.getReplayTable());

        // Replay setup
        File replayDirectory = new File(settings.getReplayLocation());
        File installDirectory = new File(settings.getInstallDirectory());
        installDirectory.mkdirs();

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

    public static App getInstance() {
        return instance;
    }

    public static void main(String[] args) {
        launch(args);
    }

}