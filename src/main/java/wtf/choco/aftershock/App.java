package wtf.choco.aftershock;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.brouillard.oss.cssfx.CSSFX;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import wtf.choco.aftershock.controller.AppController;
import wtf.choco.aftershock.files.AftershockFileOperations;
import wtf.choco.aftershock.util.FileUtil;
import wtf.choco.aftershock.keybind.KeybindRegistry;
import wtf.choco.aftershock.manager.BinRegistry;
import wtf.choco.aftershock.manager.CachingHandler;
import wtf.choco.aftershock.manager.TagRegistry;
import wtf.choco.aftershock.replay.schema.ReplayTypeAdapterFactory;
import wtf.choco.aftershock.util.ColouredLogFormatter;
import wtf.choco.aftershock.util.FXUtils;

import java.nio.file.Path;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
    @Deprecated
    private CachingHandler cacheHandler; // TODO: Replace with AftershockFileOperations (fileOperations)
    private AftershockFileOperations fileOperations;

    private BinRegistry binRegistry;
    private TagRegistry tagRegistry;

    private Path installPath;
    private Path binsPath;
    private Path replayDataPath;

    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final Logger logger = Logger.getLogger("AftershockRM");

    @Override
    public void init() throws Exception {
        instance = this;

        // Logger initialization
        this.logger.setUseParentHandlers(false);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(ColouredLogFormatter.get());
        this.logger.addHandler(handler);

        // File system initialization
        this.installPath = Path.of(System.getProperty("user.home")).resolve("AppData/Roaming/AftershockRM/");
        FileUtil.createDirectoryIfDoesntExist(installPath);

        this.binsPath = installPath.resolve("bins.json");
        this.replayDataPath = installPath.resolve("replay_data.json");
        FileUtil.createFileIfDoesntExist(binsPath);
        FileUtil.createFileIfDoesntExist(replayDataPath);

        // Misc initialization
        ApplicationSettings.init(this);
        this.cacheHandler = new CachingHandler(this);
        this.fileOperations = new AftershockFileOperations(this);
    }

    @Override
    public void start(Stage stage) {
        CSSFX.start();

        // Stage loading
        this.stage = stage;
        this.resources = ResourceBundle.getBundle("lang.", getLocale(ApplicationSettings.LOCALE.get()));

        this.binRegistry = new BinRegistry(this);
        this.tagRegistry = new TagRegistry();

        var appFXML = FXUtils.<Parent, AppController>loadFXML(AppResources.FXML_LAYOUT_ROOT.get(), resources);
        Scene scene = new Scene(appFXML.root());
        this.controller = appFXML.controller();

        // TODO: Configurable key binds
        this.keybindRegistry = new KeybindRegistry(this);
        KeybindRegistry.registerDefaultKeybinds(keybindRegistry);

        // Stage setup
        stage.setTitle("Aftershock Replay Manager v" + VERSION);
        stage.setScene(scene);
        stage.show();

        this.controller.setActiveBin(binRegistry.getGlobalBin());

        this.controller.pushProgressStatus("Loading replay data");
        this.cacheHandler.loadReplayData(replayDataPath)
                .thenAccept(loaded -> getLogger().info("Loaded Aftershock replay data for " + loaded + " replays!"))
                .thenRun(fileOperations::createDirectoriesIfNotExist)
                .thenCompose(_ -> fileOperations.performCompleteRefresh())
                .thenAcceptAsync(binRegistry.getGlobalBin().getReplays()::addAll, Platform::runLater)
                .thenCompose(_ -> binRegistry.loadBinsFromFile(binsPath))
                .thenAcceptAsync(binRegistry::addBins, Platform::runLater)
                .thenRun(controller::popProgressStatus)
                .exceptionally(e -> {
                    e.printStackTrace();
                    return null;
                });
    }

    @Override
    public void stop() throws Exception {
        this.getExecutor().shutdown();

        this.keybindRegistry.clearKeybinds();
        this.binRegistry.saveBinsToFile(binsPath);
        this.binRegistry.deleteBins(true);
        this.cacheHandler.writeReplayData(replayDataPath);
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

    public AftershockFileOperations getFileOperations() {
        return fileOperations;
    }

    public CachingHandler getCacheHandler() {
        return cacheHandler;
    }

    public ResourceBundle getResources() {
        return resources;
    }

    public ExecutorService getExecutor() {
        return executorService;
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

    public Path getInstallPath() {
        return installPath;
    }

    public void openSettingsStage() {
        if (settingsStage == null) {
            Parent root = FXUtils.loadFXMLRoot(AppResources.FXML_LAYOUT_SETTINGS_PANEL.get(), resources);
            if (root == null) {
                return;
            }

            Scene scene = new Scene(root);
            Stage stage = new Stage();
            stage.setTitle(resources.getString("ui.settings.title"));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.setScene(scene);

            this.settingsStage = stage;
        }

        this.settingsStage.show();
    }

    public void closeSettingsStage() {
        this.settingsStage.close();
    }

    private Locale getLocale(String tag) {
        // TODO: This is not safe at all and prone to exceptions. Improve this implementation
        String[] parts = tag.split("_");
        if (parts.length < 2) {
            return Locale.US;
        }

        return Locale.of(parts[0], parts[1]);
    }

    public static App getInstance() {
        return instance;
    }

    public static void main(String[] args) {
        launch(args);
    }

}