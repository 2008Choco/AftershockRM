package wtf.choco.aftershock;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.brouillard.oss.cssfx.CSSFX;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import wtf.choco.aftershock.controller.AppController;
import wtf.choco.aftershock.files.AftershockFileOperations;
import wtf.choco.aftershock.files.AftershockFileStructure;
import wtf.choco.aftershock.files.ReplayFileWatcher;
import wtf.choco.aftershock.files.ReplayFileWatcherListener;
import wtf.choco.aftershock.files.ReplayMetadataAccessor;
import wtf.choco.aftershock.keybind.KeybindRegistry;
import wtf.choco.aftershock.manager.BinRegistry;
import wtf.choco.aftershock.manager.TagRegistry;
import wtf.choco.aftershock.schema.AftershockTypeAdapterFactory;
import wtf.choco.aftershock.settings.ApplicationSettings;
import wtf.choco.aftershock.structure.ReplayBin;
import wtf.choco.aftershock.structure.ReplayEntry;
import wtf.choco.aftershock.util.ColouredLogFormatter;
import wtf.choco.aftershock.util.FXUtils;
import wtf.choco.aftershock.util.lazy.LazyValue;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class App extends Application {

    public static final Logger LOGGER = Logger.getLogger("AftershockRM");

    static {
        LOGGER.setUseParentHandlers(false);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(ColouredLogFormatter.get());
        LOGGER.addHandler(handler);
    }

    // https://www.flaticon.com
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapterFactory(AftershockTypeAdapterFactory.INSTANCE)
            .create();

    private static App instance;

    private Stage stage;
    private AppController controller;
    private ResourceBundle resources;
    private Stage settingsStage = null;
    private Stage aboutStage = null;

    private KeybindRegistry keybindRegistry;
    private AftershockFileStructure fileStructure;
    private AftershockFileOperations fileOperations;
    private ReplayFileWatcher replayFileWatcher;
    private ReplayMetadataAccessor replayMetadataAccessor;

    private final BinRegistry binRegistry = new BinRegistry();
    private final TagRegistry tagRegistry = new TagRegistry();
    private final ObjectProperty<ReplayEntry> detailedReplay = new SimpleObjectProperty<>(this, "detailedReplay"); // TODO: Find a better place to hold this :/

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    @Override
    public void init() throws Exception {
        instance = this;

        this.fileStructure = new AftershockFileStructure(Path.of(System.getProperty("user.home")).resolve("AppData/Roaming/AftershockRM/"));
        this.fileOperations = new AftershockFileOperations(this, fileStructure);

        // Misc initialization
        ApplicationSettings.init(fileStructure);

        // File watcher initialization
        this.replayFileWatcher = new ReplayFileWatcher(this);
        Thread replayFileWatcherThread = new Thread(replayFileWatcher, "Replay File Watcher");
        replayFileWatcherThread.setDaemon(true);
        replayFileWatcherThread.start();
    }

    @Override
    public void start(Stage stage) {
        CSSFX.start();

        // Stage loading
        this.stage = stage;
        this.resources = ResourceBundle.getBundle("lang.", ApplicationSettings.LOCALE.getValue());

        var appFXML = FXUtils.<Parent, AppController>loadFXML(AppResources.FXML_LAYOUT_ROOT.get(), resources);
        Scene scene = new Scene(appFXML.root());
        this.controller = appFXML.controller();

        // TODO: Configurable key binds
        this.keybindRegistry = new KeybindRegistry(this);
        KeybindRegistry.registerDefaultKeybinds(keybindRegistry);

        // Stage setup
        stage.setTitle(getAppName() + " v" + getAppVersion());
        stage.setScene(scene);
        stage.getIcons().add(AppResources.IMAGE_APP_ICON_64X.get());
        stage.show();

        this.controller.pushProgressStatus("Loading replay data");
        this.fileOperations.readReplayMetadata()
                .thenAccept(metadataStore -> this.replayMetadataAccessor = metadataStore)
                .thenCompose(_ -> fileOperations.loadReplays())
                .thenAcceptAsync(ReplayBin.GLOBAL.getReplays()::addAll, Platform::runLater)
                .thenCompose(_ -> fileOperations.readReplayBins())
                .thenAcceptAsync(binRegistry::addBins, Platform::runLater)
                .thenRun(controller::popProgressStatus)
                .exceptionally(e -> {
                    LOGGER.log(Level.SEVERE, "Error performing initial load!", e);
                    return null;
                });
    }

    @Override
    public void stop() throws Exception {
        this.getExecutor().shutdown();

        this.keybindRegistry.clearKeybinds();
        this.fileOperations.saveReplayBins();
        this.fileOperations.saveReplayMetadata();
        this.replayFileWatcher.stop();
        ApplicationSettings.save(fileStructure);

        ColouredLogFormatter.get().setLogFile(null);
    }

    public Stage getStage() {
        return stage;
    }

    public AppController getController() {
        return controller;
    }

    public AftershockFileStructure getFileStructure() {
        return fileStructure;
    }

    public AftershockFileOperations getFileOperations() {
        return fileOperations;
    }

    public ReplayFileWatcherListener getReplayFileWatcher() {
        return replayFileWatcher;
    }

    public ReplayMetadataAccessor getReplayMetadataAccessor() {
        return replayMetadataAccessor;
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

    public ObjectProperty<ReplayEntry> detailedReplayProperty() {
        return detailedReplay;
    }

    public void setDetailedReplay(ReplayEntry replay) {
        this.detailedReplayProperty().set(replay);
    }

    public ReplayEntry getDetailedReplay() {
        return detailedReplayProperty().get();
    }

    public void openSettingsStage() {
        if (settingsStage == null) {
            this.settingsStage = loadModalStage(AppResources.FXML_LAYOUT_SETTINGS_PANEL, resources.getString("ui.settings.title"));
            if (settingsStage == null) {
                return;
            }
        }

        this.settingsStage.show();
    }

    public void closeSettingsStage() {
        this.settingsStage.close();
    }

    public void openAboutStage() {
        if (aboutStage == null) {
            this.aboutStage = loadModalStage(AppResources.FXML_LAYOUT_ABOUT, resources.getString("ui.about.title").formatted(getAppName()));
            if (aboutStage == null) {
                return;
            }
        }

        this.aboutStage.show();
    }

    private Stage loadModalStage(LazyValue<URL> fxmlUrl, String title) {
        Parent root = FXUtils.loadFXMLRoot(fxmlUrl.get(), resources);
        if (root == null) {
            return null;
        }

        Scene scene = new Scene(root);
        Stage stage = new Stage();
        stage.setTitle(title);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setResizable(false);
        stage.setScene(scene);
        stage.getIcons().add(AppResources.IMAGE_APP_ICON_64X.get());

        return stage;
    }

    public void closeAboutStage() {
        this.aboutStage.close();
    }

    public static App getInstance() {
        return instance;
    }

    public static void main(String[] args) {
        launch(args);
    }

    public static String getAppName() {
        return Optional.ofNullable(App.class.getPackage().getImplementationTitle()).orElse("Aftershock Replay Manager");
    }

    public static String getAppVersion() {
        return Optional.ofNullable(App.class.getPackage().getImplementationVersion()).orElse("Dev");
    }

    public static LocalDateTime getAppBuildDate() {
        try {
            Enumeration<URL> resources = App.class.getClassLoader().getResources(JarFile.MANIFEST_NAME);
            if (!resources.hasMoreElements()) {
                return LocalDate.EPOCH.atTime(0, 0);
            }

            Manifest manifest = new Manifest(resources.nextElement().openStream());
            return Optional.ofNullable(manifest.getMainAttributes().getValue("Build-Date"))
                    .map(dateTimeString -> LocalDateTime.parse(dateTimeString, DateTimeFormatter.ISO_DATE_TIME))
                    .orElseGet(() -> LocalDate.EPOCH.atTime(0, 0));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error reading manifest file!", e);
            return LocalDate.EPOCH.atTime(0, 0);
        }
    }

}