package wtf.choco.aftershock.files;

import com.google.gson.reflect.TypeToken;
import wtf.choco.aftershock.App;
import wtf.choco.aftershock.ApplicationSettings;
import wtf.choco.aftershock.replay.Replay;
import wtf.choco.aftershock.replay.ReplayMetadata;
import wtf.choco.aftershock.structure.ReplayBin;
import wtf.choco.aftershock.structure.ReplayEntry;
import wtf.choco.aftershock.util.FileUtil;
import wtf.choco.aftershock.util.function.ThrowingSupplier;
import wtf.choco.rljp.ReplayStreamReader;
import wtf.choco.rljp.structures.ReplayHeader;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import java.util.stream.Stream;

public final class AftershockFileOperations {

    private static final String FILE_EXTENSION_REPLAY = "replay";
    private static final Predicate<Path> REPLAY_PATH_PREDICATE = path -> FileUtil.getExtension(path).equals(FILE_EXTENSION_REPLAY);

    private final App app;
    private final AftershockFileStructure fileStructure;

    public AftershockFileOperations(App app, AftershockFileStructure fileStructure) {
        this.app = app;
        this.fileStructure = fileStructure;
    }

    /**
     * Gets a {@link Path} to the directory holding live replay files that Rocket League reads from and
     * loads into the game's saved replays screen.
     * <p>
     * <strong>WARNING:</strong> Adding/removing/mutating files in this directory is destructive!
     *
     * @return a path to the live replay directory
     *
     * @see ApplicationSettings#REPLAY_DIRECTORY
     */
    public Path getLiveReplayDirectory() {
        return Path.of(ApplicationSettings.REPLAY_DIRECTORY.get());
    }

    public CompletionStage<Void> restoreUnloadedReplays(Collection<Path> unloadedReplayPaths) {
        if (!Files.isDirectory(fileStructure.unloadedReplayDirectory()) || unloadedReplayPaths.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        Path liveReplayDirectory = getLiveReplayDirectory();
        return CompletableFuture.runAsync(() -> FileUtil.createDirectoryIfDoesntExist(liveReplayDirectory), app.getExecutor())
            .thenCompose(_ -> restoreUnloadedReplaysInParallel(unloadedReplayPaths, liveReplayDirectory));
    }

    private CompletionStage<Void> restoreUnloadedReplaysInParallel(Collection<Path> unloadedReplayPaths, Path liveReplayDirectory) {
        Path unloadedReplayDirectory = fileStructure.unloadedReplayDirectory();
        try (ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<Void>> futures = unloadedReplayPaths.stream()
                .filter(path -> isValidReplay(path, unloadedReplayDirectory))
                .map(path -> CompletableFuture.runAsync(() -> restoreUnloadedReplay(path, liveReplayDirectory), virtualThreadExecutor))
                .toList();

            return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
        }
    }

    private void restoreUnloadedReplay(Path path, Path liveReplayDirectory) {
        try {
            Path targetPath = liveReplayDirectory.resolve(path.getFileName());
            Files.move(path, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new CompletionException(e);
        }
    }

    /**
     * Loads all replay files from the given replay file paths in the live or unloaded replay directories.
     *
     * @param replayPaths the paths of the replay files to load. Any non-header files or files that do not reside in the
     * live or unloaded replay directories will be ignored
     *
     * @return a future that completes when all replays have been loaded
     */
    public CompletionStage<Collection<ReplayEntry>> loadReplays(Collection<Path> replayPaths) {
        Path liveReplayDirectory = getLiveReplayDirectory();
        Path unloadedReplayDirectory = fileStructure.unloadedReplayDirectory();
        if ((!Files.isDirectory(liveReplayDirectory) && !Files.isDirectory(unloadedReplayDirectory)) || replayPaths.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        try (ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<ReplayEntry>> futures = replayPaths.stream()
                .filter(path -> isValidReplay(path, liveReplayDirectory, unloadedReplayDirectory))
                .map(path -> CompletableFuture.supplyAsync(() -> loadReplay(path), virtualThreadExecutor))
                .toList();

            return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .thenApply(_ -> futures.stream()
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .toList()
                );
        }
    }

    /**
     * Loads all known replay files in the live and unloaded replay directories.
     *
     * @return a future that completes when all replays have been loaded
     */
    public CompletionStage<Collection<ReplayEntry>> loadReplays() {
        Path liveReplayDirectory = getLiveReplayDirectory();
        Path unloadedReplayDirectory = fileStructure.unloadedReplayDirectory();
        boolean liveReplayDirectoryExists = Files.isDirectory(liveReplayDirectory);
        boolean unloadedReplayDirectoryExists = Files.isDirectory(unloadedReplayDirectory);

        // There might be a more concise way to write this
        ThrowingSupplier<Stream<Path>> fileStreamSupplier;
        if (liveReplayDirectoryExists && unloadedReplayDirectoryExists) {
            fileStreamSupplier = () -> Stream.concat(Files.list(liveReplayDirectory), Files.list(unloadedReplayDirectory));
        } else if (liveReplayDirectoryExists) {
            fileStreamSupplier = () -> Files.list(liveReplayDirectory);
        } else if (unloadedReplayDirectoryExists) {
            fileStreamSupplier = () -> Files.list(unloadedReplayDirectory);
        } else {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        try (Stream<Path> paths = fileStreamSupplier.get().filter(REPLAY_PATH_PREDICATE)) {
            return loadReplays(paths.toList());
        } catch (Exception e) {
            throw new CompletionException(e);
        }
    }

    private ReplayEntry loadReplay(Path replayPath) {
        ReplayHeader header;
        try (ReplayStreamReader reader = new ReplayStreamReader(Files.newInputStream(replayPath))) {
            header = ReplayHeader.read(reader);
        } catch (IOException e) {
            throw new CompletionException(e);
        }

        Replay replay = Replay.fromRLJPReplayHeader(header);
        ReplayMetadata replayMetadata = app.getReplayMetadataAccessor().getReplayMetadata(replay);
        Path replayFileName = replayPath.getFileName();
        Path liveReplayPath = getLiveReplayDirectory().resolve(replayFileName);
        Path unloadedReplayPath = fileStructure.unloadedReplayDirectory().resolve(replayFileName);

        return new ReplayEntry(liveReplayPath, unloadedReplayPath, replay, replayMetadata);
    }

    public CompletionStage<ReplayMetadataAccessor> readReplayMetadata() {
        return CompletableFuture.supplyAsync(() -> {
            ReplayMetadataStore metadataStore = new ReplayMetadataStore();

            Path replayMetadataPath = fileStructure.replayMetadataFile();
            if (!Files.isRegularFile(replayMetadataPath)) {
                return metadataStore;
            }

            try {
                ReplayMetadataStore store = App.GSON.fromJson(Files.newBufferedReader(replayMetadataPath, StandardCharsets.UTF_8), ReplayMetadataStore.class);
                if (store == null) {
                    store = new ReplayMetadataStore();
                }

                return store;
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        }, app.getExecutor());
    }

    // TODO: Would maybe be nice to keep this async in a CompletableFuture, but it's only ever called on shutdown where the executor has shutdown... Is it necessary?
    public void saveReplayMetadata() {
        try {
            Files.writeString(fileStructure.replayMetadataFile(), App.GSON.toJson(app.getReplayMetadataAccessor()));
        } catch (IOException e) {
            throw new CompletionException(e);
        }
    }

    public CompletionStage<List<ReplayBin>> readReplayBins() {
        return CompletableFuture.supplyAsync(() -> {
            try (BufferedReader reader = Files.newBufferedReader(fileStructure.binsFile(), StandardCharsets.UTF_8)) {
                return List.of((ReplayBin[]) App.GSON.fromJson(reader, TypeToken.getArray(ReplayBin.class)));
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        }, app.getExecutor());
    }

    // TODO: Would maybe be nice to keep this async in a CompletableFuture, but it's only ever called on shutdown where the executor has shutdown... Is it necessary?
    public void saveReplayBins() {
        try {
            List<ReplayBin> bins = app.getBinRegistry().getBins().stream()
                    .filter(Predicate.not(ReplayBin::isGlobal))
                    .toList();
            Files.writeString(fileStructure.binsFile(), App.GSON.toJson(bins), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public CompletionStage<Void> unloadReplays(Collection<Path> liveReplayPaths) {
        Path liveReplayDirectory = getLiveReplayDirectory();
        if (!Files.isDirectory(liveReplayDirectory) || liveReplayPaths.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> FileUtil.createDirectoryIfDoesntExist(fileStructure.unloadedReplayDirectory()), app.getExecutor())
            .thenCompose(_ -> unloadReplaysInParallel(liveReplayPaths, liveReplayDirectory));
    }

    private CompletionStage<Void> unloadReplaysInParallel(Collection<Path> liveReplayPaths, Path liveReplayDirectory) {
        try (ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<Void>> futures = liveReplayPaths.stream()
                .filter(path -> isValidReplay(path, liveReplayDirectory))
                .map(path -> CompletableFuture.runAsync(() -> unloadReplay(path), virtualThreadExecutor))
                .toList();

            return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
        }
    }

    private void unloadReplay(Path path) {
        try {
            Path unloadedReplayDirectory = fileStructure.unloadedReplayDirectory();
            Path unloadedReplayFileTargetPath = unloadedReplayDirectory.resolve(path.getFileName());
            Files.move(path, unloadedReplayFileTargetPath, StandardCopyOption.REPLACE_EXISTING); // Move to "unloaded"
        } catch (IOException e) {
            throw new CompletionException(e);
        }
    }

    public CompletionStage<Void> deleteReplays(Collection<Path> replayPaths) {
        Path liveReplayDirectory = getLiveReplayDirectory();
        Path unloadedReplayDirectory = fileStructure.unloadedReplayDirectory();
        if ((!Files.isDirectory(liveReplayDirectory) && !Files.isDirectory(unloadedReplayDirectory)) || replayPaths.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> FileUtil.createDirectoryIfDoesntExist(fileStructure.recentlyDeletedDirectory()), app.getExecutor())
            .thenCompose(_ -> deleteReplaysInParallel(replayPaths, liveReplayDirectory));
    }

    private CompletionStage<Void> deleteReplaysInParallel(Collection<Path> replayPaths, Path liveReplayDirectory) {
        Path unloadedReplayDirectory = fileStructure.unloadedReplayDirectory();
        try (ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<Void>> futures = replayPaths.stream()
                    .filter(path -> isValidReplay(path, liveReplayDirectory, unloadedReplayDirectory))
                    .map(path -> CompletableFuture.runAsync(() -> deleteReplay(path), virtualThreadExecutor))
                    .toList();

            return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
        }
    }

    private void deleteReplay(Path path) {
        try {
            Path recentlyDeletedDirectory = fileStructure.recentlyDeletedDirectory();
            Path recentlyDeletedFileTargetPath = recentlyDeletedDirectory.resolve(path.getFileName());
            Files.move(path, recentlyDeletedFileTargetPath, StandardCopyOption.REPLACE_EXISTING); // Move to "recently deleted"
        } catch (IOException e) {
            throw new CompletionException(e);
        }
    }

    private boolean isValidReplay(Path filePath, Path expectedDirectory) {
        return Files.isRegularFile(filePath) && FileUtil.getExtension(filePath).equals(FILE_EXTENSION_REPLAY) && filePath.startsWith(expectedDirectory);
    }

    private boolean isValidReplay(Path filePath, Path... expectedDirectories) {
        if (!Files.isRegularFile(filePath) || !FileUtil.getExtension(filePath).equals(FILE_EXTENSION_REPLAY)) {
            return false;
        }

        for (Path expectedDirectory : expectedDirectories) {
            if (filePath.startsWith(expectedDirectory)) {
                return true;
            }
        }

        return false;
    }

}
