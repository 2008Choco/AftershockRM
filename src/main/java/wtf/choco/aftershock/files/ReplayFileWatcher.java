package wtf.choco.aftershock.files;

import javafx.application.Platform;
import wtf.choco.aftershock.App;
import wtf.choco.aftershock.structure.ReplayBin;
import wtf.choco.aftershock.structure.ReplayEntry;
import wtf.choco.aftershock.util.FileUtil;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public final class ReplayFileWatcher implements Runnable, ReplayFileWatcherListener {

    private boolean running = true;
    private int ignoreIncomingEventTickets = 0;

    private final WatchService watcher;
    private final WatchKey liveReplayDirectoryKey;
    private final WatchKey unloadedReplayDirectoryKey;

    private final App app;

    public ReplayFileWatcher(App app) throws IOException {
        this.app = app;
        AftershockFileStructure fileStructure = app.getFileStructure();

        this.watcher = FileSystems.getDefault().newWatchService();

        // TODO: Support ENTRY_MODIFY
        // TODO: liveReplayDirectory() might get updated in settings, in which case this registration has to update!
        this.liveReplayDirectoryKey = fileStructure.liveReplayDirectory().register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE/*, StandardWatchEventKinds.ENTRY_MODIFY*/);
        this.unloadedReplayDirectoryKey = fileStructure.unloadedReplayDirectory().register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE/*, StandardWatchEventKinds.ENTRY_MODIFY*/);
    }

    @Override
    public void run() {
        while (running) {
            WatchKey key;
            try {
                key = watcher.take();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            if (key == null) {
                continue;
            }

            List<WatchEvent<?>> polledEvents = key.pollEvents();
            if (ignoreIncomingEventTickets > 0) {
                if (!key.reset()) {
                    break;
                }

                continue;
            }

            Map<WatchEvent.Kind<?>, List<Path>> events = new HashMap<>();
            int polledEventCount = polledEvents.size(); // It's a rough estimate. Will likely save on memory use and CPU overhead (less array resizes) in a majority of cases

            for (WatchEvent<?> untypedEvent : polledEvents) {
                WatchEvent.Kind<?> kind = untypedEvent.kind();
                if (kind == StandardWatchEventKinds.OVERFLOW) {
                    continue;
                }

                WatchEvent<Path> event = cast(untypedEvent);
                Path path = event.context();
                if (!FileUtil.getExtension(path).equals("replay")) {
                    continue;
                }

                if (key.equals(this.liveReplayDirectoryKey)) {
                    path = app.getFileStructure().liveReplayDirectory().resolve(path);
                } else if (key.equals(this.unloadedReplayDirectoryKey)) {
                    path = app.getFileStructure().unloadedReplayDirectory().resolve(path);
                } else {
                    App.LOGGER.warning("Received a file system event for an unknown watch key. Ignoring...");
                    continue;
                }

                events.computeIfAbsent(kind, _ -> new ArrayList<>(polledEventCount)).add(path);
            }

            List<Path> createdReplayFiles = events.getOrDefault(StandardWatchEventKinds.ENTRY_CREATE, Collections.emptyList());
            if (!createdReplayFiles.isEmpty()) {
                this.handleCreatedReplayFiles(createdReplayFiles);
            }

            List<Path> deletedReplayFiles = events.getOrDefault(StandardWatchEventKinds.ENTRY_DELETE, Collections.emptyList());
            if (!deletedReplayFiles.isEmpty()) {
                this.handleDeletedReplayFiles(deletedReplayFiles);
            }

            List<Path> updatedReplayFiles = events.getOrDefault(StandardWatchEventKinds.ENTRY_MODIFY, Collections.emptyList());
            if (!updatedReplayFiles.isEmpty()) {
                this.handleUpdatedReplayFiles(updatedReplayFiles);
            }

            if (!key.reset()) {
                break;
            }
        }

        try {
            this.watcher.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleCreatedReplayFiles(List<Path> paths) {
        this.app.getFileOperations().loadReplays(paths)
            .thenApply(loadedPaths -> {
                List<ReplayEntry> newReplayPaths = new ArrayList<>(loadedPaths);
                newReplayPaths.removeIf(ReplayBin.GLOBAL::contains); // Ignore any duplicate replays (theoretically impossible)
                App.LOGGER.info("Handled file system addition. Successfully loaded (" + loadedPaths.size() + ") replay(s)! (" + newReplayPaths.size() + ") are new. Adding to global replay bin.");
                return newReplayPaths;
            }).thenAcceptAsync(ReplayBin.GLOBAL.getReplays()::addAll, Platform::runLater)
            .exceptionally(e -> {
                App.LOGGER.log(Level.WARNING, "Handled file system addition. Failed to load (" + paths.size() + ") replay(s).", e);
                return null;
            });
    }

    private void handleDeletedReplayFiles(List<Path> paths) {
        List<ReplayEntry> removedReplayPaths = ReplayBin.GLOBAL.getReplays().stream()
                .filter(replay -> paths.contains(replay.getLiveReplayPath()) || paths.contains(replay.getUnloadedReplayPath()))
                .toList();

        if (!removedReplayPaths.isEmpty()) {
            App.LOGGER.info("Handled file system deletion. Removing (" + removedReplayPaths.size() + ") replay(s) from all replay bins.");
            Platform.runLater(() -> app.getBinRegistry().getBins().forEach(bin -> bin.getReplays().removeAll(removedReplayPaths)));
        }
    }

    private void handleUpdatedReplayFiles(List<Path> paths) {
        // TODO: Refresh ReplayEntry data? Unsure how to go about this...
    }

    @SuppressWarnings("unchecked")
    private static WatchEvent<Path> cast(WatchEvent<?> untypedEvent) {
        return (WatchEvent<Path>) untypedEvent;
    }

    public void stop() {
        this.running = false;
    }

    @Override
    public void pushIgnoreIncomingEventsTicket() {
        this.ignoreIncomingEventTickets++;
    }

    @Override
    public void popIgnoreIncomingEventsTicket() {
        if (this.ignoreIncomingEventTickets <= 0) {
            throw new IllegalStateException("Attempted to pop an ignore incoming events ticket when there are no tickets to pop!");
        }

        this.ignoreIncomingEventTickets--;
    }

}
