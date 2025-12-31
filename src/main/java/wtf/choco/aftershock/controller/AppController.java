package wtf.choco.aftershock.controller;

import java.awt.Toolkit;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import wtf.choco.aftershock.App;
import wtf.choco.aftershock.ApplicationSettings;
import wtf.choco.aftershock.manager.BinRegistry;
import wtf.choco.aftershock.manager.CachingHandler;
import wtf.choco.aftershock.replay.Team;
import wtf.choco.aftershock.structure.DynamicFilter;
import wtf.choco.aftershock.structure.EditableTextTableCell;
import wtf.choco.aftershock.structure.ReplayBin;
import wtf.choco.aftershock.structure.ReplayEntry;
import wtf.choco.aftershock.structure.ReplayPropertyFetcher;
import wtf.choco.aftershock.structure.StringListTableCell;
import wtf.choco.aftershock.structure.Tag;
import wtf.choco.aftershock.structure.bin.BinEditor;
import wtf.choco.aftershock.util.FXUtils;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TableView.TableViewSelectionModel;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;

public final class AppController {

    public static final DataFormat DATA_FORMAT_REPLAY_ENTRY = new DataFormat();

    private static final Image DRAG_IMAGE = new Image(App.class.getResourceAsStream("/icons/file.png"));

    @FXML private TableView<ReplayEntry> replayTable;

    @FXML private TableColumn<ReplayEntry, Boolean> columnLoaded;
    @FXML private TableColumn<ReplayEntry, String> columnReplayName;
    @FXML private TableColumn<ReplayEntry, String> columnLastModified;
    @FXML private TableColumn<ReplayEntry, String> columnMode;
    @FXML private TableColumn<ReplayEntry, Integer> columnScoreBlue;
    @FXML private TableColumn<ReplayEntry, Integer> columnScoreOrange;
    @FXML private TableColumn<ReplayEntry, String> columnMap;
    @FXML private TableColumn<ReplayEntry, String> columnOwner;
    @FXML private TableColumn<ReplayEntry, String> columnComments;
    @FXML private TableColumn<ReplayEntry, List<Tag>> columnTags;

    @FXML private SplitPane splitPane;

    @FXML private Label labelListed, labelLoaded, labelSelected;
    @FXML private TextField filterBar;
    @FXML private ImageView filterOptionsImage;

    @FXML private Label progressStatus;
    @FXML private ProgressBar progressBar;

    @FXML private ResourceBundle resources;

    @FXML private HBox primaryDisplay;
    @FXML private VBox binEditorPane, binEditorList;
    @FXML private ScrollPane binEditorScrollPane;

    private BinEditor binEditor;

    private double lastDividerPositionInfo = 0.70;

    private Popup popup = new Popup();

    private DynamicFilter<ReplayEntry> tableFilter = new DynamicFilter<>((r, t) -> r.getReplay().getName().toLowerCase().contains(t.toLowerCase()));

    @FXML
    public void initialize() {
        App app = App.getInstance();

        this.columnLoaded.setCellFactory(CheckBoxTableCell.forTableColumn(columnLoaded));
        this.columnLoaded.setCellValueFactory(new PropertyValueFactory<>("loaded"));
        this.columnReplayName.setCellValueFactory(new ReplayPropertyFetcher<>(r -> r.getReplay().getName()));
        this.columnLastModified.setCellValueFactory(new ReplayPropertyFetcher<>(r -> r.getReplay().getDate().toString().replace('T', ' ')));
        this.columnMode.setCellValueFactory(new ReplayPropertyFetcher<>(r -> String.format("%dv%1$d", r.getReplay().getTeamSize())));
        this.columnScoreBlue.setCellValueFactory(new ReplayPropertyFetcher<>(r -> r.getReplay().getScore(Team.BLUE)));
        this.columnScoreOrange.setCellValueFactory(new ReplayPropertyFetcher<>(r -> r.getReplay().getScore(Team.ORANGE)));
        this.columnMap.setCellValueFactory(new ReplayPropertyFetcher<>(r -> r.getReplay().getMapName()));
        this.columnOwner.setCellValueFactory(new ReplayPropertyFetcher<>(r -> r.getReplay().getPlayerName()));
        this.columnComments.setCellFactory(EditableTextTableCell.getFactoryCallback("None"));
        this.columnComments.setCellValueFactory(new PropertyValueFactory<>("comments"));
        this.columnTags.setCellFactory(StringListTableCell.getFactoryCallback());
        this.columnTags.setCellValueFactory(new PropertyValueFactory<>("tags"));

        TableViewSelectionModel<ReplayEntry> selectionModel = replayTable.getSelectionModel();
        selectionModel.setSelectionMode(SelectionMode.MULTIPLE);

        // TODO: Do this with CSS
        StackPane root = FXUtils.loadFXMLRoot("/layout/FilterPopup", resources);
        this.popup.setAutoHide(true);
        this.popup.getContent().add(root);
        this.popup.setOnHidden(e -> filterOptionsImage.setOpacity(0.5));
        this.popup.setWidth(root.getPrefWidth());
        this.popup.setHeight(root.getPrefHeight());

        this.filterOptionsImage.setCursor(Cursor.HAND);

        // Label update listeners
        this.replayTable.itemsProperty().addListener((c, oldValue, newValue) -> setLabel(labelListed, "ui.footer.listed", newValue.size()));

        selectionModel.getSelectedItems().addListener((ListChangeListener<ReplayEntry>) c -> {
            if (!c.next()) {
                return;
            }

            this.setLabel(labelSelected, "ui.footer.selected", c.getList().size());
            if (c.getAddedSize() > 0) {
                this.openInfoPanel(c.getAddedSubList().get(0));
                this.splitPane.setDividerPosition(splitPane.getDividers().size() - 1, lastDividerPositionInfo);
            }
        });

        BinRegistry.GLOBAL_BIN.getReplaysObservable().addListener((ListChangeListener<ReplayEntry>) c -> updateLoadedLabel());

        this.replayTable.setOnMouseClicked(e -> replayTable.requestFocus());
        this.replayTable.setOnDragDetected(e -> {
            var selection = replayTable.getSelectionModel();
            if (selection.isEmpty()) {
                return;
            }

            Dragboard dragboard = replayTable.startDragAndDrop(TransferMode.COPY_OR_MOVE);
            dragboard.setDragView(DRAG_IMAGE);

            ClipboardContent clipboard = new ClipboardContent();
            StringBuilder replays = new StringBuilder();
            List<File> files = new ArrayList<>(selection.getSelectedItems().size());
            for (ReplayEntry replay : selection.getSelectedItems()) {
                replays.append(replay.getReplay().getId());
                replays.append(";");

                files.add(replay.getReplay().getDemoFile());
            }

            clipboard.putFiles(files);
            clipboard.putString(replays.toString().substring(0, replays.length() - 1));
            dragboard.setContent(clipboard);
        });

        this.replayTable.setOnDragOver(e -> {
            Dragboard dragboard = e.getDragboard();
            if (e.getGestureSource() == replayTable) {
                return;
            }

            if (dragboard.hasFiles()) {
                for (File file : dragboard.getFiles()) {
                    if (!file.getName().endsWith(".replay")) {
                        return;
                    }
                }

                e.acceptTransferModes(TransferMode.COPY);
            }

            else if (dragboard.hasUrl()) {
                String url = dragboard.getUrl();
                if (!url.endsWith(".replay")) {
                    return;
                }

                e.acceptTransferModes(TransferMode.COPY);
            }
        });

        this.replayTable.setOnDragDropped(e -> {
            Dragboard dragboard = e.getDragboard();
            if (dragboard.hasFiles()) {
                List<File> files = dragboard.getFiles();
                files.removeIf(f -> BinRegistry.GLOBAL_BIN.hasReplay(f.getName().substring(0, f.getName().lastIndexOf('.'))));

                if (files.size() >= 1) {
                    CachingHandler cacheHandler = App.getInstance().getCacheHandler();
                    app.getTaskExecutor().execute(t -> {
                        String replayDirectory = app.getSettings().get(ApplicationSettings.REPLAY_DIRECTORY);
                        files.forEach(f -> {
                            File demoFile = new File(replayDirectory, f.getName());

                            try {
                                Files.copy(f.toPath(), demoFile.toPath());
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                        });

                        cacheHandler.cacheReplays(t, files);
                        cacheHandler.loadReplays(t, files);
                    });
                }

                e.setDropCompleted(true);
            }

            else if (dragboard.hasUrl()) {
                String urlRaw = dragboard.getUrl();

                URL url = null;
                try {
                    url = new URL(urlRaw);
                } catch (MalformedURLException ex) {
                    app.getLogger().warning("Malformed URL. Could not download replay");
                    ex.printStackTrace();
                }

                if (url == null) {
                    return;
                }

                final URL urlFinal = url; // Stupid lambdas...
                app.getTaskExecutor().execute(t -> {
                    String replayName = urlRaw.substring(urlRaw.lastIndexOf('/') + 1);
                    t.updateMessage("Fetching file...");

                    try (ReadableByteChannel readableByteChannel = Channels.newChannel(urlFinal.openStream())) {
                        File file = new File(app.getSettings().get(ApplicationSettings.REPLAY_DIRECTORY), replayName);
                        if (!file.createNewFile()) {
                            return;
                        }

                        t.updateMessage("Downloading " + replayName.substring(0, replayName.lastIndexOf('.')) + "...");
                        t.updateProgress(1, 4);

                        Logger logger = app.getLogger();
                        logger.info("Downloading file \"" + replayName + "\" (from " + urlFinal + ")");

                        FileOutputStream fileOutputStream = new FileOutputStream(file);
                        fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
                        fileOutputStream.close();
                        logger.info("Done");

                        t.updateMessage("Caching replay headers...");
                        t.updateProgress(2, 4);

                        List<File> toCache = Arrays.asList(file);
                        CachingHandler cacheHandler = App.getInstance().getCacheHandler();
                        cacheHandler.cacheReplays(null, toCache);

                        t.updateMessage("Loading replay...");
                        t.updateProgress(3, 4);
                        cacheHandler.loadReplays(null, toCache);

                        t.updateProgress(4, 4);
                    } catch (IOException ex) {
                        app.getLogger().warning("Could not complete the download due to an IO exception:");
                        ex.printStackTrace();
                    }
                });

                e.setDropCompleted(true);
            }
        });

        this.binEditor = new BinEditor(app, this, binEditorPane, binEditorList);

        ContextMenu binEditorContextMenu = new ContextMenu();
        MenuItem showHiddenBins = new MenuItem("Unhide bins");
        showHiddenBins.setOnAction(e -> new ArrayList<>(binEditor.getHidden()).forEach(binEditor::unhide));
        binEditorContextMenu.getItems().add(showHiddenBins);
        this.binEditorScrollPane.setContextMenu(binEditorContextMenu);

        // TODO: Add functionality for "open file location"
        ContextMenu tableContextMenu = new ContextMenu();
        MenuItem openWithReplayEditor = new MenuItem("Open with Replay Editor...");
        openWithReplayEditor.setOnAction(e -> {
            String replayEditorPath = app.getSettings().get(ApplicationSettings.REPLAY_EDITOR_PATH);
            if (replayEditorPath == null || replayEditorPath.isBlank()) {
                return;
            }

            try {
                new ProcessBuilder().command(replayEditorPath, "-open", replayTable.getSelectionModel().getSelectedItems().get(0).getReplay().getDemoFile().getAbsolutePath())
                    .redirectError(Redirect.DISCARD).redirectOutput(Redirect.DISCARD).start(); // Do something with the output instead. Write to file?
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        tableContextMenu.getItems().addAll(new MenuItem("Open file location..."), openWithReplayEditor);

        Menu sendTo = new Menu("Send to...");
        MenuItem separator = new SeparatorMenuItem();

        this.replayTable.setContextMenu(tableContextMenu);

        this.replayTable.setOnContextMenuRequested(e -> {
            if (replayTable.getSelectionModel().isEmpty()) {
                tableContextMenu.hide();
                e.consume();
                return;
            }

            String replayEditorPath = app.getSettings().get(ApplicationSettings.REPLAY_EDITOR_PATH);
            openWithReplayEditor.setDisable(replayEditorPath == null || replayEditorPath.isBlank());

            BinRegistry binRegistry = app.getBinRegistry();

            tableContextMenu.getItems().add(2, separator);
            tableContextMenu.getItems().add(3, sendTo);

            sendTo.getItems().clear();

            for (ReplayBin bin : binRegistry.getBins()) {
                if (bin.isGlobalBin() || bin == binEditor.getDisplayed()) {
                    continue;
                }

                MenuItem item = new MenuItem(bin.getName());
                item.setOnAction(itemEvent -> replayTable.getSelectionModel().getSelectedItems().forEach(i -> bin.addReplay(i.getReplay())));
                sendTo.getItems().add(item);
            }

            if (sendTo.getItems().isEmpty()) {
                tableContextMenu.getItems().remove(3);
                tableContextMenu.getItems().remove(2);
            }
        });

        // Zero the labels on init (no placeholder %s should be visible)
        this.setLabel(labelListed, "ui.footer.listed", 0);
        this.setLabel(labelSelected, "ui.footer.selected", 0);
    }

    @FXML
    public void tableKeyboardControl(KeyEvent event) {
        KeyCode key = event.getCode();

        if (key == KeyCode.A && event.isControlDown()) {
            replayTable.getSelectionModel().selectAll();
        }

        else if (key == KeyCode.SPACE) {
            var selectionModel = replayTable.getSelectionModel();
            if (selectionModel.isEmpty()) {
                return;
            }

            selectionModel.getSelectedItems().forEach(e -> e.setLoaded(!e.isLoaded()));
        }

        else if (key == KeyCode.DELETE) {
            var selection = replayTable.getSelectionModel();
            if (selection.isEmpty()) {
                return;
            }

            ReplayBin displayed = binEditor.getDisplayed();
            if (displayed == null || displayed.isGlobalBin()) {
                Toolkit.getDefaultToolkit().beep();
                return;
            }

            List<ReplayEntry> selected = new ArrayList<>(selection.getSelectedItems());
            selection.clearSelection();
            selected.forEach(r -> displayed.removeReplay(r.getReplay()));
            this.closeInfoPanel();
        }
    }

    @FXML
    public void openLink(ActionEvent event) {
        String toOpen = null;
        switch (((Hyperlink) event.getTarget()).getId()) {
            case "donation":
                toOpen = "https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=hawkeboyz%40hotmail.com&currency_code=USD&source=Aftershock";
                break;
            case "source":
                toOpen = "https://www.github.com/2008Choco/AftershockRM";
                break;
        }

        App.getInstance().getHostServices().showDocument(toOpen);
    }

    @FXML
    public void openSettings(@SuppressWarnings("unused") ActionEvent event) {
        App.getInstance().openSettingsStage();
    }

    @FXML
    public void toggleBinEditor(@SuppressWarnings("unused") ActionEvent event) {
        ObservableList<Node> primaryDisplayChildren = primaryDisplay.getChildren();
        if (primaryDisplayChildren.size() == 1) {
            primaryDisplayChildren.add(0, binEditor.getNode());
        } else {
            primaryDisplayChildren.remove(binEditor.getNode());
        }
    }

    @FXML
    public void createBin(@SuppressWarnings("unused") ActionEvent event) {
        int duplicateCount = 0;
        String name = "New Bin";

        BinRegistry binRegistry = App.getInstance().getBinRegistry();
        ReplayBin bin = null;
        while ((bin = binRegistry.createBin(name + (duplicateCount++ >= 1 ? " (" + duplicateCount + ")" : ""))) == null);

        this.binEditor.getSelectionModel().clearSelection();
        this.binEditor.display(bin);
        bin.getDisplay().openNameEditor();
    }

    @FXML
    public void deleteBin(@SuppressWarnings("unused") ActionEvent event) {
        this.binEditor.deleteBins(binEditor.getSelectionModel().getSelectedItems(), true);
    }

    @FXML
    public void updateFilter(@SuppressWarnings("unused") KeyEvent event) {
        DynamicFilter<ReplayEntry> filter = getTableFilter();
        filter.setTerm(filterBar.getText());

        if (filter.isInvalid()) {
            this.replayTable.setItems(binEditor.getDisplayed().getReplaysObservable());
            return;
        }

        ObservableList<ReplayEntry> items = replayTable.getItems();
        if (!(items instanceof FilteredList)) {
            this.replayTable.setItems(items = new FilteredList<>(binEditor.getDisplayed().getReplaysObservable(), filter));
        }

        FilteredList<ReplayEntry> filteredItems = (FilteredList<ReplayEntry>) items;
        filteredItems.setPredicate(null); // Must set to null first to invalidate the predicate... stupid
        filteredItems.setPredicate(getTableFilter());
    }

    @FXML
    public void exitFilter(KeyEvent event) {
        KeyCode pressed = event.getCode();
        if (pressed == KeyCode.ESCAPE || pressed == KeyCode.ENTER) {
            this.replayTable.requestFocus();
            event.consume();
            return;
        }
    }

    @FXML
    public void toggleFilterMenu(MouseEvent event) {
        Object clicked = event.getSource();
        if (clicked instanceof ImageView) {
            ImageView clickedImage = (ImageView) clicked;

            if (!popup.isShowing()) {
                clickedImage.setOpacity(1.0);

                Bounds imageBounds = clickedImage.localToScreen(clickedImage.getBoundsInLocal());
                this.popup.show(clickedImage, imageBounds.getCenterX() - (popup.getWidth() / 2), imageBounds.getCenterY() - 15 - popup.getHeight());
            } else {
                clickedImage.setOpacity(0.5);
                this.popup.hide();
            }
        }
    }

    public TableView<ReplayEntry> getReplayTable() {
        return replayTable;
    }

    public ProgressBar getProgressBar() {
        return progressBar;
    }

    public Label getProgressStatus() {
        return progressStatus;
    }

    public TextField getFilterBar() {
        return filterBar;
    }

    public BinEditor getBinEditor() {
        return binEditor;
    }

    public DynamicFilter<ReplayEntry> getTableFilter() {
        return tableFilter;
    }

    public void closeInfoPanel() {
        ObservableList<Node> items = splitPane.getItems();
        if (items.get(items.size() - 1) != replayTable) {
            this.lastDividerPositionInfo = splitPane.getDividerPositions()[splitPane.getDividers().size() - 1];
            items.remove(items.size() - 1);
        }
    }

    public void openInfoPanel(ReplayEntry replay) {
        this.closeInfoPanel();
        this.splitPane.getItems().add(replay.getReplay().getInfoPanel());
    }

    public void updateLoadedLabel() {
        int loaded = 0;
        for (ReplayEntry replay : BinRegistry.GLOBAL_BIN.getReplays()) {
            if (replay.isLoaded()) {
                loaded++;
            }
        }

        this.setLabel(labelLoaded, "ui.footer.loaded", loaded);
    }

    private void setLabel(Label label, String resourceKey, int amount) {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> setLabel(label, resourceKey, amount));
            return;
        }

        label.setText(String.format(resources.getString(resourceKey), amount));
    }

}
