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
import wtf.choco.aftershock.keybind.KeybindRegistry;
import wtf.choco.aftershock.manager.ReplayManager;
import wtf.choco.aftershock.replay.ReplayModifiable;
import wtf.choco.aftershock.util.ColouredLogFormatter;
import wtf.choco.aftershock.util.FXUtils;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.stage.Modality;
import javafx.stage.Stage;

public final class App extends Application {

    private static boolean isDevelopment = false;

    // https://www.flaticon.com
    private static final Gson GSON = new Gson();
    private static App instance;

    private Stage stage;
    private Scene scene;
    private AppController controller;
    private ResourceBundle resources;
    private KeybindRegistry keybindRegistry;

    private Stage settingsStage = null;

    private final ReplayManager replayManager = new ReplayManager();
    private File installDirectory;

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

        if (isDevelopment) {
            this.installDirectory = new File(System.getProperty("user.home"), "AppData/Roaming/AftershockRM/");
            this.logger.warning("Running app in development mode. Installation directory will be at " + installDirectory.getPath());
        } else {
            this.installDirectory = new File(".");
        }
    }

    @Override
    public void start(Stage stage) throws IOException {
        this.stage = stage;
        var root = FXUtils.<Parent, AppController>loadFXML("/layout/Root", resources = ResourceBundle.getBundle("lang."));
        this.scene = new Scene(root.getKey());
        this.controller = root.getValue();

        // TODO: Configurable key binds
        this.keybindRegistry = new KeybindRegistry(this);
        this.keybindRegistry.registerKeybind(KeyCode.ESCAPE).executes(controller::closeInfoPanel);

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

            this.controller.requestLabelUpdate();
            this.replayManager.addChangeListener(l -> {
                if (l.next()) {
                    this.controller.increaseLoadedReplay(l.getAddedSize());
                }
            });

            this.executor.execute(() -> {
                File[] replayFiles = replayDirectory.listFiles((f, name) -> name.endsWith(".replay"));
                this.controller.setExpectedReplays(replayFiles.length);

                for (File replayFile : replayFiles) {
                    String replayFileName = replayFile.getName();

                    // Backup the replay file for later
                    File cacheDestination = new File(replayCacheDirectory, replayFileName);
                    if (!cacheDestination.exists()) {
                        this.logger.info("(" + formatID(replayFileName) + ") - Caching replay file");
                        try {
                            Files.copy(replayFile.toPath(), cacheDestination.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    File headerDestination = new File(replayHeadersDirectory, replayFileName.substring(0, replayFileName.lastIndexOf('.')) + ".json");
                    ReplayModifiable replay = new ReplayModifiable(cacheDestination, headerDestination);

                    if (!headerDestination.exists()) {
                        this.logger.info("(" + formatID(replayFileName) + ") - Creating header file");

                        try {
                            Runtime.getRuntime().exec(settings.getRattletrapPath() + " --f --i \"" + replayFile.getAbsolutePath() + "\" --o \"" + headerDestination.getAbsolutePath() + "\"").waitFor();
                        } catch (InterruptedException | IOException e) {
                            e.printStackTrace();
                        }

                        this.logger.info("Done!");
                    }

                    replay.loadDataFromFile(GSON);
                    this.replayManager.addReplay(replay);
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

    private String formatID(String id) {
        int idLength = id.length();
        StringBuilder formatted = new StringBuilder(idLength);
        formatted.append(id.substring(0, 4));
        formatted.append("...");
        formatted.append(id.substring(idLength - ".replay".length() - 4));
        return formatted.toString();
    }

    public static App getInstance() {
        return instance;
    }

    public static void main(String[] args) {
        for (String arg : args) {
            if (arg.equals("--dev")) {
                isDevelopment = true;
            }
        }

        launch(args);
    }

}