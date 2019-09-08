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
import com.google.gson.GsonBuilder;

import wtf.choco.aftershock.controller.AppController;
import wtf.choco.aftershock.keybind.KeybindRegistry;
import wtf.choco.aftershock.manager.BinRegistry;
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

    // https://www.flaticon.com
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static App instance;

    private Stage stage;
    private Scene scene;
    private AppController controller;
    private ResourceBundle resources;
    private Stage settingsStage = null;

    private KeybindRegistry keybindRegistry;
    private ApplicationSettings settings;

    private final BinRegistry binRegistry = new BinRegistry();
    private File installDirectory;

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

        if (getParameters().getRaw().contains("--dev")) {
            this.installDirectory = new File(System.getProperty("user.home"), "AppData/Roaming/AftershockRM/");
            this.logger.warning("Running app in development mode. Installation directory will be at " + installDirectory.getPath());
        } else {
            this.installDirectory = new File(".");
        }

        this.settings = new ApplicationSettings(this);
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

        this.controller.displayBin(BinRegistry.GLOBAL_BIN);

        // Replay setup
        this.installDirectory.mkdirs();
        this.reloadReplayFiles();
    }

    @Override
    public void stop() throws Exception {
        this.executor.shutdown();
        this.binRegistry.clearBins(false);
        this.settings.writeToFile();
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

    public ExecutorService getExecutor() {
        return executor;
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

    public void reloadReplayFiles() {
        this.binRegistry.clearBins(true);
        BinRegistry.GLOBAL_BIN.clear();
        this.controller.requestLabelUpdate();

        File replayCacheDirectory = new File(installDirectory, "Cache");
        File replayHeadersDirectory = new File(installDirectory, "Headers");
        replayCacheDirectory.mkdir();
        replayHeadersDirectory.mkdir();

        String replayDirectoryPath = settings.get(ApplicationSettings.REPLAY_DIRECTORY);
        if (replayDirectoryPath != null && !replayDirectoryPath.isBlank()) {
            File replayDirectory = new File(replayDirectoryPath);

            if (replayDirectory.exists() && replayDirectory.isDirectory()) {
                this.logger.info("Running startup replay caching processes...");
                long now = System.currentTimeMillis();

                this.executor.execute(() -> {
                    File[] replayFiles = replayDirectory.listFiles((f, name) -> name.endsWith(".replay"));
                    this.controller.prepareLoading(replayFiles.length);

                    for (File replayFile : replayFiles) {
                        String replayFileName = replayFile.getName();

                        // Backup the replay file for later
                        File cacheDestination = new File(replayCacheDirectory, replayFileName);
                        if (!cacheDestination.exists()) {
                            this.logger.info("(" + truncateID(replayFileName) + ") - Caching replay file");
                            try {
                                Files.copy(replayFile.toPath(), cacheDestination.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        File headerDestination = new File(replayHeadersDirectory, replayFileName.substring(0, replayFileName.lastIndexOf('.')) + ".json");
                        ReplayModifiable replay = new ReplayModifiable(cacheDestination, headerDestination);

                        if (!headerDestination.exists()) {
                            this.logger.info("(" + truncateID(replayFileName) + ") - Creating header file");

                            try {
                                Runtime.getRuntime().exec(settings.get(ApplicationSettings.RATTLETRAP_PATH) + " --f --i \"" + replayFile.getAbsolutePath() + "\" --o \"" + headerDestination.getAbsolutePath() + "\"").waitFor();
                            } catch (InterruptedException | IOException e) {
                                e.printStackTrace();
                            }

                            this.logger.info("Done!");
                        }

                        replay.loadDataFromFile();
                        BinRegistry.GLOBAL_BIN.addReplay(replay);
                    }

                    long time = System.currentTimeMillis() - now;
                    this.logger.info("Loaded " + BinRegistry.GLOBAL_BIN.size() + " replays in " + time + " milliseconds!");
                    this.controller.requestLabelUpdate();
                });
            } else {
                this.logger.warning("Could not find replay directory at path " + settings.get(ApplicationSettings.REPLAY_DIRECTORY));
            }
        }
    }

    public static String truncateID(String id) {
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
        launch(args);
    }

}