package wtf.choco.aftershock;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.ConsoleHandler;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import wtf.choco.aftershock.controller.AppController;
import wtf.choco.aftershock.controller.BinEditorController;
import wtf.choco.aftershock.keybind.KeybindRegistry;
import wtf.choco.aftershock.manager.BinRegistry;
import wtf.choco.aftershock.manager.CachingHandler;
import wtf.choco.aftershock.manager.TagRegistry;
import wtf.choco.aftershock.util.ColouredLogFormatter;
import wtf.choco.aftershock.util.FXUtils;

import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
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

    private Parent binEditorPane;
    private BinEditorController binEditorController;

    private KeybindRegistry keybindRegistry;
    private ApplicationSettings settings;
    private CachingHandler cacheHandler;

    private final BinRegistry binRegistry = new BinRegistry();
    private final TagRegistry tagRegistry = new TagRegistry();
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

        // POST INSTALL DIRECTORY STARTUP

        this.settings = new ApplicationSettings(this);
        this.cacheHandler = new CachingHandler(this);
    }

    @Override
    public void start(Stage stage) throws IOException {
        // Stage loading
        this.stage = stage;
        var root = FXUtils.<Parent, AppController>loadFXML("/layout/Root", resources = ResourceBundle.getBundle("lang."));
        this.scene = new Scene(root.getKey());
        this.controller = root.getValue();

        var binEditorRoot = FXUtils.<Parent, BinEditorController>loadFXML("/layout/BinEditor", resources);
        this.binEditorPane = binEditorRoot.getKey();
        this.binEditorController = binEditorRoot.getValue();

        // TODO: Configurable key binds
        this.keybindRegistry = new KeybindRegistry(this);
        this.keybindRegistry.registerKeybind(KeyCode.ESCAPE).executes(() -> {
            this.controller.closeInfoPanel();
            this.controller.getReplayTable().getSelectionModel().clearSelection();
        });
        this.keybindRegistry.registerKeybind(KeyCode.A, KeyCombination.CONTROL_DOWN).executes(() -> controller.getReplayTable().getSelectionModel().selectAll());
        this.keybindRegistry.registerKeybind(KeyCode.SPACE).or(KeyCode.ENTER).executes(() -> {
            var selectionModel = this.controller.getReplayTable().getSelectionModel();
            if (selectionModel.isEmpty()) {
                return;
            }

            selectionModel.getSelectedItems().forEach(e -> e.setLoaded(!e.isLoaded()));
        });

        // Stage setup
        stage.setTitle("Aftershock Replay Manager");
        stage.setScene(scene);
        stage.show();

        this.controller.displayBin(BinRegistry.GLOBAL_BIN);

        // Replay setup
        this.installDirectory.mkdirs();
        this.reloadReplays();

        this.binRegistry.createBin("Testing Bin");
    }

    @Override
    public void stop() throws Exception {
        this.executor.shutdown();
        this.binRegistry.clearBins(false);
        this.tagRegistry.clearTags();
        this.settings.writeToFile();
    }

    public Logger getLogger() {
        return logger;
    }

    public Stage getStage() {
        return stage;
    }

    public AppController getController() {
        return controller;
    }

    public ResourceBundle getResources() {
        return resources;
    }

    public Parent getBinEditorPane() {
        return binEditorPane;
    }

    public BinEditorController getBinEditorController() {
        return binEditorController;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public BinRegistry getBinRegistry() {
        return binRegistry;
    }

    public TagRegistry getTagRegistry() {
        return tagRegistry;
    }

    public ApplicationSettings getSettings() {
        return settings;
    }

    public File getInstallDirectory() {
        return installDirectory;
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

    public void reloadReplays() {
        this.executor.execute(() -> {
            this.cacheHandler.cacheReplays();
            this.cacheHandler.loadReplaysFromCache();
        });
    }

    public static String truncateID(String id) {
        int idLength = id.length();
        StringBuilder formatted = new StringBuilder(idLength);
        formatted.append(id.substring(0, 4));
        formatted.append("...");
        formatted.append(id.substring(idLength - ".replay".length() - 4));
        formatted.trimToSize();
        return formatted.toString();
    }

    public static App getInstance() {
        return instance;
    }

    public static void main(String[] args) {
        launch(args);
    }

}