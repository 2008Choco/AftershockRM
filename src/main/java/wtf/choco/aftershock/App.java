package wtf.choco.aftershock;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import wtf.choco.aftershock.controller.AppController;
import wtf.choco.aftershock.keybind.KeybindRegistry;
import wtf.choco.aftershock.manager.BinRegistry;
import wtf.choco.aftershock.manager.CachingHandler;
import wtf.choco.aftershock.manager.ProgressiveTaskExecutor;
import wtf.choco.aftershock.manager.TagRegistry;
import wtf.choco.aftershock.replay.schema.ReplayTypeAdapterFactory;
import wtf.choco.aftershock.structure.bin.BinDisplayComponent;
import wtf.choco.aftershock.util.ColouredLogFormatter;
import wtf.choco.aftershock.util.FXUtils;

import java.io.File;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.logging.ConsoleHandler;
import java.util.logging.Logger;

public final class App extends Application {

    // https://www.flaticon.com
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapterFactory(new ReplayTypeAdapterFactory())
            .create();

    public static final String VERSION = "0.1.1A";

    private static App instance;

    private Stage stage;
    private AppController controller;
    private ResourceBundle resources;
    private Stage settingsStage = null;

    private KeybindRegistry keybindRegistry;
    private ProgressiveTaskExecutor taskExecutor;
    private CachingHandler cacheHandler;

    private final BinRegistry binRegistry = new BinRegistry();
    private final TagRegistry tagRegistry = new TagRegistry();

    private File installDirectory;
    private File binsFile;
    private File replayDataFile;

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
        this.installDirectory = new File(System.getProperty("user.home"), "AppData/Roaming/AftershockRM/");

        // Post-install directory initialization
        this.installDirectory.mkdirs();
        this.binsFile = new File(installDirectory, "bins.json");
        this.binsFile.createNewFile();
        this.replayDataFile = new File(installDirectory, "replay_data.json");
        this.replayDataFile.createNewFile();

        ApplicationSettings.init(this);
        this.cacheHandler = new CachingHandler(this);
    }

    @Override
    public void start(Stage stage) {
        // Stage loading
        this.stage = stage;
        this.resources = ResourceBundle.getBundle("lang.", getLocale(ApplicationSettings.LOCALE.get()));

        var appFXML = FXUtils.<Parent, AppController>loadFXML("/layout/Root", resources);
        Scene scene = new Scene(appFXML.root());
        this.controller = appFXML.controller();

        this.taskExecutor = new ProgressiveTaskExecutor(primaryExecutor, controller.getProgressBar(), controller.getProgressStatus());

        // TODO: Configurable key binds
        this.keybindRegistry = new KeybindRegistry(this);
        KeybindRegistry.registerDefaultKeybinds(keybindRegistry);

        // Listen for clicks outside of bin name editor, cancel editing
        scene.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            PickResult result = event.getPickResult();

            this.binRegistry.getBins().forEach(bin -> {
                BinDisplayComponent binDisplay = bin.getDisplay();
                if (binDisplay.isBeingEdited() && !(result.getIntersectedNode() instanceof Text)) {
                    binDisplay.closeNameEditor(true);
                }
            });
        });

        // Stage setup
        stage.setTitle("Aftershock Replay Manager v" + VERSION);
        stage.setScene(scene);
        stage.show();

        this.controller.getBinEditor().display(BinRegistry.GLOBAL_BIN);

        // Replay setup
        this.reloadReplays((_, _) -> {
            Platform.runLater(() -> binRegistry.loadBinsFromFile(binsFile, false));
        });
    }

    @Override
    public void stop() throws Exception {
        this.primaryExecutor.shutdown();
        this.pooledExecutor.shutdown();

        this.keybindRegistry.clearKeybinds();
        this.binRegistry.saveBinsToFile(binsFile);
        this.binRegistry.deleteBins(true);
        this.cacheHandler.writeReplayData(replayDataFile);
        this.tagRegistry.clearTags();
        ApplicationSettings.save(this);

        ColouredLogFormatter.get().setLogFile(null);
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

    public ProgressiveTaskExecutor getTaskExecutor() {
        return taskExecutor;
    }

    public BinRegistry getBinRegistry() {
        return binRegistry;
    }

    public TagRegistry getTagRegistry() {
        return tagRegistry;
    }

    public KeybindRegistry getKeybindRegistry() {
        return keybindRegistry;
    }

    public File getInstallDirectory() {
        return installDirectory;
    }

    public void openSettingsStage() {
        if (settingsStage == null) {
            Parent root = FXUtils.loadFXMLRoot("/layout/SettingsPanel", resources);
            if (root == null) {
                return;
            }

            Scene scene = new Scene(root);
            Stage stage = new Stage();
            stage.setTitle("Aftershock Replay Manager - Application Settings");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.setScene(scene);

            this.settingsStage = stage;
        }

        this.settingsStage.show();
    }

    public void closeSettingsStage() {
        this.settingsStage.close();
        this.settingsStage = null;
    }

    public void reloadReplays(BiConsumer<?, Worker.State> whenCompleted) {
        this.taskExecutor.execute(task -> {
            this.cacheHandler.cacheReplays(task);
            try {
                this.cacheHandler.loadReplayData(replayDataFile);
                this.cacheHandler.loadReplaysFromCache(task);
            } catch (Exception e) {
                // TODO: This really sucks. I need better exception handling!
                e.printStackTrace();
            }
        }, whenCompleted, primaryExecutor);
    }

    private Locale getLocale(String tag) {
        // TODO: This is not safe at all and prone to exceptions. Improve this implementation
        String[] parts = tag.split("_");
        if (parts.length < 2) {
            return Locale.US;
        }

        return Locale.of(parts[0], parts[1]);
    }

    public static String truncateID(String id) {
        if (id.endsWith(".replay")) {
            id = id.substring(0, id.lastIndexOf('.'));
        }

        return id.substring(0, 4) + "..." + id.substring(id.length() - 4);
    }

    public static App getInstance() {
        return instance;
    }

    public static void main(String[] args) {
        launch(args);
    }

}