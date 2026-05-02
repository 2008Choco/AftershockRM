package wtf.choco.aftershock.controller;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.property.ListProperty;
import javafx.beans.value.ObservableIntegerValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TableView.TableViewSelectionModel;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Popup;
import wtf.choco.aftershock.App;
import wtf.choco.aftershock.AppResources;
import wtf.choco.aftershock.ApplicationSettings;
import wtf.choco.aftershock.control.ReplayBinDisplayPane;
import wtf.choco.aftershock.files.AftershockFileOperations;
import wtf.choco.aftershock.replay.IReplay;
import wtf.choco.aftershock.replay.Team;
import wtf.choco.aftershock.structure.EditableTextTableCell;
import wtf.choco.aftershock.structure.ReplayBin;
import wtf.choco.aftershock.structure.ReplayEntry;
import wtf.choco.aftershock.structure.ReplayPropertyFetcher;
import wtf.choco.aftershock.structure.StringListTableCell;
import wtf.choco.aftershock.structure.Tag;
import wtf.choco.aftershock.util.ComplexBindings;
import wtf.choco.aftershock.util.FXUtils;
import wtf.choco.aftershock.util.ReplayTableFilter;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.ResourceBundle;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class AppController {

    @FXML private TableView<ReplayEntry> replayTable;

    @FXML private TableColumn<ReplayEntry, Boolean> columnLoaded;
    @FXML private TableColumn<ReplayEntry, String> columnReplayName;
    @FXML private TableColumn<ReplayEntry, String> columnDate;
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
    @FXML private ReplayBinDisplayPane replayBinDisplayPane;

    private double lastDividerPositionInfo = 0.70;
    private final Deque<String> progressStates = new ArrayDeque<>();

    private final Popup popup = new Popup();

    private ReplayTableFilter tableFilter;

    @FXML
    public void initialize() {
        this.columnLoaded.setCellFactory(CheckBoxTableCell.forTableColumn(columnLoaded));
        this.columnLoaded.setCellValueFactory(new PropertyValueFactory<>("loaded"));
        this.columnReplayName.setCellValueFactory(new ReplayPropertyFetcher<>(IReplay::name));
        this.columnDate.setCellValueFactory(new ReplayPropertyFetcher<>(replay -> replay.date().toString().replace('T', ' ')));
        this.columnMode.setCellValueFactory(new ReplayPropertyFetcher<>(replay -> String.format("%dv%1$d", replay.teamSize())));
        this.columnScoreBlue.setCellValueFactory(new ReplayPropertyFetcher<>(replay -> replay.score(Team.BLUE)));
        this.columnScoreOrange.setCellValueFactory(new ReplayPropertyFetcher<>(replay -> replay.score(Team.ORANGE)));
        this.columnMap.setCellValueFactory(new ReplayPropertyFetcher<>(replay -> replay.mapName(resources)));
        this.columnOwner.setCellValueFactory(new ReplayPropertyFetcher<>(IReplay::playerName));
        this.columnComments.setCellFactory(EditableTextTableCell.getFactoryCallback("None"));
        this.columnComments.setCellValueFactory(new PropertyValueFactory<>("comments"));
        this.columnTags.setCellFactory(StringListTableCell.getFactoryCallback());
        this.columnTags.setCellValueFactory(new PropertyValueFactory<>("tags"));

        TableViewSelectionModel<ReplayEntry> selectionModel = replayTable.getSelectionModel();
        selectionModel.setSelectionMode(SelectionMode.MULTIPLE);

        // TODO: Do this with CSS
        StackPane root = FXUtils.loadFXMLRoot(AppResources.FXML_LAYOUT_FILTER_POPUP.get(), resources);
        this.popup.setAutoHide(true);
        this.popup.getContent().add(root);
        this.popup.setOnHidden(_ -> filterOptionsImage.setOpacity(0.25));
        this.popup.setOnShown(_ -> filterOptionsImage.setOpacity(1.0));
        this.popup.setWidth(root.getPrefWidth());
        this.popup.setHeight(root.getPrefHeight());

        this.filterOptionsImage.setCursor(Cursor.HAND);

        selectionModel.getSelectedItems().addListener(this::onSelectedItemsChange);

        this.tableFilter = new ReplayTableFilter(ReplayBin.GLOBAL);
        this.tableFilter.replayBinProperty().bind(replayBinDisplayPane.activeBinProperty());
        this.tableFilter.searchTermProperty().bind(filterBar.textProperty());

        ListProperty<ReplayEntry> replays = ReplayBin.GLOBAL.replaysProperty();
        FilteredList<ReplayEntry> filteredReplays = new FilteredList<>(replays, tableFilter);
        SortedList<ReplayEntry> sortedReplays = new SortedList<>(filteredReplays);
        this.replayTable.setItems(sortedReplays);
        sortedReplays.comparatorProperty().bind(replayTable.comparatorProperty());

        this.replayTable.setOnMouseClicked(_ -> replayTable.requestFocus());
        this.replayTable.setOnDragDetected(_ -> onReplayTableDragStart());
        this.replayTable.setOnDragOver(this::onReplayTableDragEnter);
        this.replayTable.setOnDragDropped(this::onReplayTableDragDropped);

        this.replayTable.setContextMenu(createReplayTableContextMenu());
        this.replayTable.setOnContextMenuRequested(this::onReplayTableContextMenuRequested);

        // Refresh the table when the search term changes and when we change active bins
        ListChangeListener<ReplayEntry> forceRefilterListener = _ -> forceRefilter(filteredReplays, tableFilter);
        this.replayBinDisplayPane.activeBinProperty().addListener((_, oldValue, newValue) -> {
            this.forceRefilter(filteredReplays, tableFilter);

            if (oldValue != null) {
                oldValue.replaysProperty().removeListener(forceRefilterListener);
            }

            if (newValue != null) {
                newValue.replaysProperty().addListener(forceRefilterListener);
            }
        });
        this.tableFilter.searchTermProperty().addListener(_ -> forceRefilter(filteredReplays, tableFilter));

        ObservableIntegerValue loadedCount = ComplexBindings.createIntegerBindingCountingBooleanProperties(replayTable.getItems(), ReplayEntry::loadedProperty);
        this.labelListed.textProperty().bind(Bindings.size(replayTable.getItems()).map(listed -> resources.getString("ui.footer.listed").formatted(listed)));
        this.labelLoaded.textProperty().bind(loadedCount.map(loaded -> resources.getString("ui.footer.loaded").formatted(loaded)));
        this.labelSelected.textProperty().bind(Bindings.size(replayTable.getSelectionModel().getSelectedItems()).map(selected -> resources.getString("ui.footer.selected").formatted(selected)));
    }

    /*
     * Unfortunately, FilteredList#refilter() is private, so to get around this we force a call to it by invalidating
     * the property by setting it to null then re-setting it to our filter. It sucks, but it's the only way unless I
     * want to invoke refilter() via reflection...
     * ...
     * ...
     * which isn't the worst idea? :)
     */
    private <T> void forceRefilter(FilteredList<T> list, Predicate<T> filter) {
        list.setPredicate(null);
        list.setPredicate(filter);
    }

    private void onSelectedItemsChange(ListChangeListener.Change<? extends ReplayEntry> change) {
        if (!change.next()) {
            return;
        }

        if (change.getAddedSize() > 0) {
            this.openInfoPanel(change.getAddedSubList().getFirst());
            this.splitPane.setDividerPosition(splitPane.getDividers().size() - 1, lastDividerPositionInfo);
        }
    }

    private void onReplayTableDragStart() {
        var selection = replayTable.getSelectionModel();
        if (selection.isEmpty()) {
            return;
        }

        Dragboard dragboard = replayTable.startDragAndDrop(TransferMode.COPY_OR_MOVE);
        dragboard.setDragView(AppResources.IMAGE_FILE.get());

        ClipboardContent clipboard = new ClipboardContent();
        StringJoiner replays = new StringJoiner(";");
        List<File> files = new ArrayList<>(selection.getSelectedItems().size());
        for (ReplayEntry replay : selection.getSelectedItems()) {
            replays.add(replay.id());
            files.add(replay.getLiveReplayPath().toFile());
        }

        clipboard.putFiles(files);
        clipboard.putString(replays.toString());
        dragboard.setContent(clipboard);
    }

    private void onReplayTableDragEnter(DragEvent event) {
        Dragboard dragboard = event.getDragboard();
        if (event.getGestureSource() == replayTable) {
            return;
        }

        if (dragboard.hasFiles()) {
            for (File file : dragboard.getFiles()) {
                if (!file.getName().endsWith(".replay")) {
                    return;
                }
            }

            event.acceptTransferModes(TransferMode.COPY);
        } else if (dragboard.hasUrl()) {
            try {
                String replayName = getReplayNameFromURI(URI.create(dragboard.getUrl().trim()));
                if (!replayName.endsWith(".replay")) {
                    return;
                }

                event.acceptTransferModes(TransferMode.COPY);
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private void onReplayTableDragDropped(DragEvent event) {
        App app = App.getInstance();
        Dragboard dragboard = event.getDragboard();

        CompletionStage<Collection<Path>> dragOperation;
        if (dragboard.hasFiles()) {
            dragOperation = copyReplayFilesToLiveReplayDirectory(dragboard.getFiles());
            event.setDropCompleted(true);
        } else if (dragboard.hasUrl()) {
            try {
                URI uri = URI.create(dragboard.getUrl().trim());
                dragOperation = downloadReplayFileFromURL(uri).thenApply(result -> {
                    app.getLogger().info("Finished downloading replay from URL: " + uri);
                    return result;
                });
            } catch (IllegalArgumentException e) {
                dragOperation = CompletableFuture.failedFuture(e);
            }

            event.setDropCompleted(true);
        } else {
            dragOperation = CompletableFuture.completedFuture(Collections.emptyList());
        }

        AftershockFileOperations fileOperations = app.getFileOperations();
        dragOperation.thenCompose(fileOperations::createReplayBackups)
                .thenCompose(fileOperations::generateHeaders)
                .thenCompose(fileOperations::loadHeaders)
                .thenAcceptAsync(ReplayBin.GLOBAL.getReplays()::addAll, Platform::runLater)
                .exceptionally(e -> {
                    e.printStackTrace();
                    return null;
                });
    }

    private CompletionStage<Collection<Path>> copyReplayFilesToLiveReplayDirectory(Collection<File> files) {
        if (files.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        return CompletableFuture.supplyAsync(() -> {
            List<Path> newPaths = new ArrayList<>(files.size());

            Path liveReplayDirectory = App.getInstance().getFileOperations().getLiveReplayDirectory();
            for (File file : files) {
                Path targetPath = liveReplayDirectory.resolve(file.getName());
                try {
                    Files.copy(file.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
                    newPaths.add(targetPath);
                } catch (IOException e) {
                    throw new CompletionException(e);
                }
            }

            return newPaths;
        }, App.getInstance().getExecutor());
    }

    private CompletionStage<Collection<Path>> downloadReplayFileFromURL(URI uri) {
        String replayName = getReplayNameFromURI(uri);
        return CompletableFuture.supplyAsync(() -> {
            Path targetPath = App.getInstance().getFileOperations().getLiveReplayDirectory().resolve(replayName);
            try (ReadableByteChannel channelIn = Channels.newChannel(uri.toURL().openStream());
                 FileChannel channelOut = FileChannel.open(targetPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)
            ) {
                channelOut.transferFrom(channelIn, 0, Long.MAX_VALUE);
                return List.of(targetPath);
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        }, App.getInstance().getExecutor());
    }

    private String getReplayNameFromURI(URI uri) {
        String path = uri.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }

    private ContextMenu createReplayTableContextMenu() {
        MenuItem openFileLocation = new MenuItem(resources.getString("ui.table.context_menu.open_file_location"));
        openFileLocation.setOnAction(_ -> onOpenFileLocation());

        MenuItem openWithReplayEditor = new MenuItem(resources.getString("ui.table.context_menu.open_with_replay_editor"));
        openWithReplayEditor.setOnAction(_ -> onOpenWithReplayEditor());
        openWithReplayEditor.disableProperty().bind(ApplicationSettings.REPLAY_EDITOR_PATH.property().isEmpty());

        // "Send to..." bins menu item (conditional!)
        // We only want to add the "Send to..." context menu if there is at least one bin
        ObservableList<ReplayBin> bins = App.getInstance().getBinRegistry().getBins();
        IntegerBinding binCountBinding = Bindings.size(bins);
        BooleanBinding propertyHasSufficientBins = replayBinDisplayPane.activeBinProperty().isEqualTo(ReplayBin.GLOBAL).and(binCountBinding.greaterThan(1))
            .or(replayBinDisplayPane.activeBinProperty().isNotEqualTo(ReplayBin.GLOBAL).and(binCountBinding.greaterThan(2)));

        MenuItem separator = new SeparatorMenuItem();
        Menu sendTo = new Menu(resources.getString("ui.table.context_menu.send_to"));
        separator.visibleProperty().bindBidirectional(sendTo.visibleProperty());
        sendTo.visibleProperty().bind(propertyHasSufficientBins);

        bins.addListener((InvalidationListener) _ -> {
            sendTo.getItems().clear();
            for (ReplayBin bin : bins) {
                // We don't want to have a "Send to global bin" option, this doesn't make any sense. All replays are in the global bin
                if (bin.isGlobal()) {
                    continue;
                }

                MenuItem item = new MenuItem(bin.getName());
                item.setOnAction(_ -> replayTable.getSelectionModel().getSelectedItems().forEach(replay -> {
                    ObservableList<ReplayEntry> replays = bin.getReplays();
                    if (!replays.contains(replay)) {
                        replays.add(replay);
                    }
                }));
                item.visibleProperty().bind(replayBinDisplayPane.activeBinProperty().isNotEqualTo(bin));
                sendTo.getItems().add(item);
            }
        });

        return new ContextMenu(openFileLocation, openWithReplayEditor, separator, sendTo);
    }

    private void onOpenWithReplayEditor() {
        String replayEditorPath = ApplicationSettings.REPLAY_EDITOR_PATH.get();
        if (replayEditorPath == null || replayEditorPath.isBlank()) {
            return;
        }

        try {
            String replayPath = replayTable.getSelectionModel().getSelectedItems().getFirst().getLiveReplayPath().toAbsolutePath().toString();
            new ProcessBuilder().command(replayEditorPath, "-open", replayPath)
                    .redirectError(Redirect.DISCARD)
                    .redirectOutput(Redirect.DISCARD)
                    .start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void onOpenFileLocation() {
        try {
            String selectedReplayFilePath = replayTable.getSelectionModel().getSelectedItems().getFirst().getLiveReplayPath().toAbsolutePath().toString();
            new ProcessBuilder("explorer.exe", "/select,", selectedReplayFilePath)
                    .redirectOutput(Redirect.DISCARD)
                    .redirectError(Redirect.DISCARD)
                    .start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void onReplayTableContextMenuRequested(ContextMenuEvent event) {
        // If there are no selected items, we don't need to show the context menu
        if (replayTable.getSelectionModel().isEmpty()) {
            this.replayTable.getContextMenu().hide();
            event.consume();
        }
    }

    @FXML
    public void tableKeyboardControl(KeyEvent event) {
        KeyCode key = event.getCode();

        if (key == KeyCode.A && event.isControlDown()) {
            replayTable.getSelectionModel().selectAll();
        } else if (key == KeyCode.SPACE) {
            var selectionModel = replayTable.getSelectionModel();
            if (selectionModel.isEmpty()) {
                return;
            }

            selectionModel.getSelectedItems().forEach(replay -> replay.setLoaded(!replay.isLoaded()));
        } else if (key == KeyCode.DELETE) {
            var selection = replayTable.getSelectionModel();
            if (selection.isEmpty()) {
                return;
            }

            ReplayBin activeBin = replayBinDisplayPane.getActiveBin();
            if (activeBin == null) {
                return;
            }

            // Show a prompt for global bin deletions
            boolean deleteLiveReplays = activeBin.isGlobal();
            if (deleteLiveReplays) {
                String singleOrMultiple = selection.getSelectedItems().size() == 1 ? "single" : "multiple";
                String replayNames = selection.getSelectedItems().stream().map(IReplay::name).collect(Collectors.joining(", "));
                if (!promptDeleteLiveReplayFiles(singleOrMultiple, replayNames, resources)) {
                    return;
                }
            }

            List<ReplayEntry> selected = new ArrayList<>(selection.getSelectedItems());
            selection.clearSelection();

            if (deleteLiveReplays) {
                App.getInstance().getFileOperations().deleteReplays(selected.stream().map(ReplayEntry::getLiveReplayPath).toList(), true)
                        .thenRun(() -> App.getInstance().getLogger().info("Deleted " + selected.size() + " live replays! They've been added to the RecentlyDeleted directory!"))
                        .exceptionally(e -> {
                            e.printStackTrace();
                            return null;
                        });
            }

            selected.forEach(activeBin.getReplays()::remove);
            this.closeInfoPanel();
        }
    }

    private boolean promptDeleteLiveReplayFiles(String singleOrMultiple, String replayName, ResourceBundle resources) {
        Alert confirmation = new Alert(Alert.AlertType.WARNING);
        confirmation.setTitle(resources.getString("ui.table.delete_replay.confirm." + singleOrMultiple + ".title"));
        confirmation.setHeaderText(resources.getString("ui.table.delete_replay.confirm." + singleOrMultiple + ".header"));
        confirmation.setContentText(resources.getString("ui.table.delete_replay.confirm." + singleOrMultiple + ".content").formatted(replayName));

        ButtonType buttonDelete = new ButtonType(resources.getString("ui.table.delete_replay.confirm." + singleOrMultiple + ".delete"));
        ButtonType buttonCancel = new ButtonType(resources.getString("ui.table.delete_replay.confirm." + singleOrMultiple + ".cancel"));
        confirmation.getButtonTypes().setAll(buttonDelete, buttonCancel);

        return confirmation.showAndWait().orElse(buttonCancel) != buttonCancel;
    }

    @FXML
    public void openLink(ActionEvent event) {
        String toOpen = switch (((Hyperlink) event.getTarget()).getId()) {
            case "donation" -> "https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=hawkeboyz%40hotmail.com&currency_code=USD&source=Aftershock";
            case "source" -> "https://www.github.com/2008Choco/AftershockRM";
            default -> null;
        };

        if (toOpen != null) {
            App.getInstance().getHostServices().showDocument(toOpen);
        }
    }

    @FXML
    public void openSettings(@SuppressWarnings("unused") ActionEvent event) {
        App.getInstance().openSettingsStage();
    }

    @FXML
    public void toggleBinEditor(@SuppressWarnings("unused") ActionEvent event) {
        // TODO: Toggle the visible property instead
        ObservableList<Node> primaryDisplayChildren = primaryDisplay.getChildren();
        if (primaryDisplayChildren.size() == 1) {
            primaryDisplayChildren.addFirst(replayBinDisplayPane);
        } else {
            primaryDisplayChildren.remove(replayBinDisplayPane);
        }
    }

    @FXML
    public void exitFilter(KeyEvent event) {
        KeyCode pressed = event.getCode();
        if (pressed == KeyCode.ESCAPE || pressed == KeyCode.ENTER) {
            this.replayTable.requestFocus();
            event.consume();
        }
    }

    @FXML
    public void toggleFilterMenu(MouseEvent event) {
        if (!(event.getSource() instanceof ImageView image)) {
            return;
        }

        if (!popup.isShowing()) {
            Bounds imageBounds = image.localToScreen(image.getBoundsInLocal());
            this.popup.show(image, imageBounds.getCenterX() - (popup.getWidth() / 2), imageBounds.getCenterY() - 15 - popup.getHeight());
        } else {
            this.popup.hide();
        }
    }

    public TableView<ReplayEntry> getReplayTable() {
        return replayTable;
    }

    public void pushProgressStatus(String message) {
        boolean shouldMakeVisible = progressStates.isEmpty();
        this.progressStatus.setText(message);
        this.progressStates.push(message);

        if (shouldMakeVisible) {
            this.progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
            this.progressBar.setVisible(true);
            this.progressStatus.setVisible(true);
        }
    }

    public void popProgressStatus() {
        this.progressStates.pollLast();

        String currentStatus = progressStates.peekLast();
        if (currentStatus == null) {
            this.progressBar.setVisible(false);
            this.progressStatus.setVisible(false);
        } else {
            this.progressStatus.setText(currentStatus);
        }
    }

    public TextField getFilterBar() {
        return filterBar;
    }

    public void openInfoPanel(ReplayEntry replay) {
        this.closeInfoPanel();
        this.splitPane.getItems().add(replay.getInfoPanel(resources));
    }

    public void closeInfoPanel() {
        ObservableList<Node> items = splitPane.getItems();
        if (items.getLast() != replayTable) {
            this.lastDividerPositionInfo = splitPane.getDividerPositions()[splitPane.getDividers().size() - 1];
            items.removeLast();
        }
    }

}
