package wtf.choco.aftershock;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.ConsoleHandler;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import wtf.choco.aftershock.controller.AppController;
import wtf.choco.aftershock.keybind.KeybindRegistry;
import wtf.choco.aftershock.manager.BinRegistry;
import wtf.choco.aftershock.manager.CachingHandler;
import wtf.choco.aftershock.manager.TagRegistry;
import wtf.choco.aftershock.structure.ReplayEntry;
import wtf.choco.aftershock.structure.bin.BinDisplayComponent;
import wtf.choco.aftershock.util.ColouredLogFormatter;
import wtf.choco.aftershock.util.FXUtils;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Pair;

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
    private CachingHandler cacheHandler;

    private final BinRegistry binRegistry = new BinRegistry();
    private final TagRegistry tagRegistry = new TagRegistry();

    private File installDirectory;
    private File binsFile;

    private final ExecutorService primaryExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService pooledExecutor = Executors.newCachedThreadPool();
    private final Logger logger = Logger.getLogger("AftershockRM");

    @Override
    public void init() throws Exception {
        instance = this;

        // Logger initialization
        this.logger.setUseParentHandlers(false);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(ColouredLogFormatter.get());
        this.logger.addHandler(handler);

        // Install directory initialization
        if (getParameters().getRaw().contains("--dev")) {
            this.installDirectory = new File(System.getProperty("user.home"), "AppData/Roaming/AftershockRM/");
            this.logger.warning("Running app in development mode. Installation directory will be at " + installDirectory.getPath());
        } else {
            this.installDirectory = new File(".");
        }

        // Post-install directory initialization
        this.binsFile = new File(installDirectory, "bins.json");
        this.binsFile.createNewFile();

        this.settings = new ApplicationSettings(this);
        this.cacheHandler = new CachingHandler(this);
    }

    @Override
    public void start(Stage stage) throws IOException {
        // Stage loading
        this.stage = stage;
        Pair<Parent, AppController> root = FXUtils.loadFXML("/layout/Root", resources = ResourceBundle.getBundle("lang.", getLocale(settings.get(ApplicationSettings.LOCALE))));
        this.scene = new Scene(root.getKey());
        this.controller = root.getValue();

        // TODO: Configurable key binds
        this.keybindRegistry = new KeybindRegistry(this);
        KeybindRegistry.registerDefaultKeybinds(keybindRegistry);

        // Listen for clicks outside of bin name editor, cancel editing
        this.scene.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
            PickResult result = e.getPickResult();

            this.binRegistry.getBins().forEach(b -> {
                BinDisplayComponent binDisplay = b.getDisplay();
                if (binDisplay.isBeingEdited() && !(result.getIntersectedNode() instanceof Text)) {
                    binDisplay.closeNameEditor(true);
                }
            });
        });

        // Stage setup
        stage.setTitle("Aftershock Replay Manager");
        stage.setScene(scene);
        stage.show();

        this.controller.getBinEditor().display(BinRegistry.GLOBAL_BIN);

        // Replay setup
        this.installDirectory.mkdirs();
        this.reloadReplays().thenRun(() -> Platform.runLater(() -> binRegistry.loadBinsFromFile(binsFile, false)));
    }

    @Override
    public void stop() throws Exception {
        this.primaryExecutor.shutdown();
        this.pooledExecutor.shutdown();

        this.keybindRegistry.clearKeybinds();
        this.binRegistry.saveBinsToFile(binsFile);
        this.binRegistry.deleteBins(true);
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

    public CachingHandler getCacheHandler() {
        return cacheHandler;
    }

    public ResourceBundle getResources() {
        return resources;
    }

    public ExecutorService getExecutor() {
        return primaryExecutor;
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

    public CompletableFuture<Void> reloadReplays() {
        return CompletableFuture.runAsync(() -> {
            this.cacheHandler.cacheReplays();
            this.cacheHandler.loadReplaysFromCache();
        }, primaryExecutor);
    }

    public void processReplayIO(ReplayEntry replay) {
        this.pooledExecutor.execute(replay::writeToHeader);
    }

    private Locale getLocale(String tag) {
        String[] parts = tag.split("_");
        if (parts.length < 2) {
            return null;
        }

        return new Locale(parts[0], parts[1]);
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