package wtf.choco.aftershock.files;

import com.google.gson.reflect.TypeToken;
import wtf.choco.aftershock.App;
import wtf.choco.aftershock.ApplicationSettings;
import wtf.choco.aftershock.replay.Replay;
import wtf.choco.aftershock.replay.ReplayMetadata;
import wtf.choco.aftershock.structure.ReplayBin;
import wtf.choco.aftershock.structure.ReplayEntry;
import wtf.choco.aftershock.util.FileUtil;
import wtf.choco.rljp.ReplayStreamReader;
import wtf.choco.rljp.structures.ReplayHeader;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

    private static final ThreadLocal<MessageDigest> MD5 = ThreadLocal.withInitial(() -> {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Couldn't get the MD5 hashing algorithm. How old is this computer?", e);
        }
    });

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

    /**
     * Gets a {@link Collection} of {@link Path Paths} to replay files in the {@link #getLiveReplayDirectory()
     * live replay directory} (limited to the provided paths) whose corresponding file in the replay backup
     * directory do not match the signature of the live file, or do not exist in the replay backup directory at
     * all.
     *
     * @param liveReplayPaths the live replay paths to check
     *
     * @return a future that completes when all replays have been checked
     */
    public CompletionStage<Collection<Path>> getMismatchingReplayBackupPaths(Collection<Path> liveReplayPaths) {
        // If the live replay directory doesn't exist, then we won't consider it "mismatching" because the original is gone
        Path liveReplayDirectory = getLiveReplayDirectory();
        if (!Files.isDirectory(liveReplayDirectory)) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        // If the replay backups directory doesn't exist, we can assume that all replay paths are mismatching. They have no corresponding backup replay
        Path replayBackupsDirectory = fileStructure.replayBackupsDirectory();
        if (!Files.isDirectory(replayBackupsDirectory)) {
            return CompletableFuture.completedFuture(liveReplayPaths.stream()
                .filter(path -> !isInvalidPath(path, liveReplayDirectory, FILE_EXTENSION_REPLAY))
                .toList()
            );
        }

        try (ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<Path>> futures = liveReplayPaths.stream()
                .filter(path -> !isInvalidPath(path, liveReplayDirectory, FILE_EXTENSION_REPLAY))
                .map(path -> CompletableFuture.supplyAsync(() -> hasMismatchedBackupReplay(path) ? path : null, virtualThreadExecutor))
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
     * Gets a {@link Collection} of {@link Path Paths} to replay files in the {@link #getLiveReplayDirectory()
     * live replay directory} whose corresponding file in the replay backup directory do not match the signature
     * of the live file, or do not exist in the replay backup directory at all.
     * <p>
     * This method's stage result is most useful as a parameter to {@link #createReplayBackups(Collection)}.
     *
     * @return a future that completes when all replays have been checked
     */
    public CompletionStage<Collection<Path>> getMismatchingReplayBackupPaths() {
        Path liveReplayDirectory = getLiveReplayDirectory();
        if (!Files.isDirectory(liveReplayDirectory)) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        try (Stream<Path> paths = Files.list(liveReplayDirectory).filter(REPLAY_PATH_PREDICATE)) {
            return getMismatchingReplayBackupPaths(paths.toList());
        } catch (IOException e) {
            throw new CompletionException(e);
        }
    }

    private boolean hasMismatchedBackupReplay(Path liveReplayPath) {
        Path replayBackupsDirectory = fileStructure.replayBackupsDirectory();
        if (!Files.isDirectory(replayBackupsDirectory)) {
            return true;
        }

        Path backupReplayFilePath = replayBackupsDirectory.resolve(liveReplayPath.getFileName());
        if (!Files.isRegularFile(backupReplayFilePath)) {
            return true;
        }

        try {
            MessageDigest md5 = MD5.get();
            byte[] liveReplayHash = md5.digest(Files.readAllBytes(liveReplayPath));
            byte[] backupReplayHash = md5.digest(Files.readAllBytes(backupReplayFilePath));
            return !MessageDigest.isEqual(liveReplayHash, backupReplayHash);
        } catch (IOException e) {
            throw new CompletionException(e);
        }
    }

    /**
     * Creates backups for the given replay file paths in the {@link #getLiveReplayDirectory() live replay directory}
     * into the replay backup directory. If a replay already exists in the replay backup directory, it will be
     * overwritten with the replay file from the live replay directory.
     *
     * @param liveReplayPaths the paths for which to create replay backup files. Any non-replay files or files that do
     * not reside in the live replay directory will be ignored
     *
     * @return a future that completes when the backup process has finished
     */
    public CompletionStage<Collection<Path>> createReplayBackups(Collection<Path> liveReplayPaths) {
        Path liveReplayDirectory = getLiveReplayDirectory();
        if (!Files.isDirectory(liveReplayDirectory) || liveReplayPaths.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        return CompletableFuture.runAsync(() -> FileUtil.createDirectoryIfDoesntExist(fileStructure.replayBackupsDirectory()), app.getExecutor())
            .thenCompose(_ -> createReplayBackupsInParallel(liveReplayPaths, liveReplayDirectory));
    }

    private CompletionStage<Collection<Path>> createReplayBackupsInParallel(Collection<Path> liveReplayPaths, Path liveReplayDirectory) {
        try (ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<Path>> futures = liveReplayPaths.stream()
                .filter(path -> !isInvalidPath(path, liveReplayDirectory, FILE_EXTENSION_REPLAY))
                .map(path -> CompletableFuture.supplyAsync(() -> createReplayBackup(path), virtualThreadExecutor))
                .toList();

            return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .thenApply(_ -> futures.stream()
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .toList()
                );
        }
    }

    private Path createReplayBackup(Path path) {
        Path targetPath = fileStructure.replayBackupsDirectory().resolve(path.getFileName());
        boolean changed = !Files.isRegularFile(targetPath);
        try {
            byte[] hash = null;
            if (!changed) {
                hash = MD5.get().digest(Files.readAllBytes(path));
            }

            Files.copy(path, targetPath, StandardCopyOption.REPLACE_EXISTING);

            if (hash != null) {
                changed = !MessageDigest.isEqual(hash, MD5.get().digest(Files.readAllBytes(path)));
            }

            return changed ? targetPath : null;
        } catch (IOException e) {
            throw new CompletionException(e);
        }
    }

    public CompletionStage<Void> restoreReplayBackups(Collection<Path> replayBackupPaths) {
        if (!Files.isDirectory(fileStructure.replayBackupsDirectory()) || replayBackupPaths.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        Path liveReplayDirectory = getLiveReplayDirectory();
        return CompletableFuture.runAsync(() -> FileUtil.createDirectoryIfDoesntExist(liveReplayDirectory), app.getExecutor())
            .thenCompose(_ -> restoreReplayBackupsInParallel(replayBackupPaths, liveReplayDirectory));
    }

    private CompletionStage<Void> restoreReplayBackupsInParallel(Collection<Path> replayBackupPaths, Path liveReplayDirectory) {
        Path replayBackupsDirectory = fileStructure.replayBackupsDirectory();
        try (ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<Void>> futures = replayBackupPaths.stream()
                .filter(path -> !isInvalidPath(path, replayBackupsDirectory, FILE_EXTENSION_REPLAY))
                .map(path -> CompletableFuture.runAsync(() -> restoreReplayBackup(path, liveReplayDirectory), virtualThreadExecutor))
                .toList();

            return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
        }
    }

    private void restoreReplayBackup(Path path, Path liveReplayDirectory) {
        try {
            Path targetPath = liveReplayDirectory.resolve(path.getFileName());
            Files.copy(path, targetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
        } catch (IOException e) {
            throw new CompletionException(e);
        }
    }

    /**
     * Loads all replay files from the given replay file paths in the replay backups directory.
     *
     * @param replayBackupPaths the paths of the replay files to load. Any non-header files or files that do not reside in the
     * replay backups directory will be ignored
     *
     * @return a future that completes when all replays have been loaded
     */
    public CompletionStage<Collection<ReplayEntry>> loadReplays(Collection<Path> replayBackupPaths) {
        Path replayBackupsDirectory = fileStructure.replayBackupsDirectory();
        if (!Files.isDirectory(replayBackupsDirectory) || replayBackupPaths.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        return CompletableFuture.runAsync(() -> FileUtil.createDirectoryIfDoesntExist(replayBackupsDirectory), app.getExecutor())
            .thenCompose(_ -> loadReplaysInParallel(replayBackupPaths));
    }

    private CompletionStage<Collection<ReplayEntry>> loadReplaysInParallel(Collection<Path> replayBackupPaths) {
        try (ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<ReplayEntry>> futures = replayBackupPaths.stream()
                .filter(path -> !isInvalidPath(path, fileStructure.replayBackupsDirectory(), FILE_EXTENSION_REPLAY))
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
     * Loads all replay files in the replay backups directory.
     *
     * @return a future that completes when all replays have been loaded
     */
    public CompletionStage<Collection<ReplayEntry>> loadReplays() {
        Path replayBackupsDirectory = fileStructure.replayBackupsDirectory();
        if (!Files.isDirectory(replayBackupsDirectory)) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        try (Stream<Path> paths = Files.list(replayBackupsDirectory).filter(REPLAY_PATH_PREDICATE)) {
            return loadReplays(paths.toList());
        } catch (IOException e) {
            throw new CompletionException(e);
        }
    }

    private ReplayEntry loadReplay(Path replayBackupPath) {
        ReplayHeader header;
        try (ReplayStreamReader reader = new ReplayStreamReader(Files.newInputStream(replayBackupPath))) {
            header = ReplayHeader.read(reader);
        } catch (IOException e) {
            throw new CompletionException(e);
        }

        Replay replay = Replay.fromRLJPReplayHeader(header);
        ReplayMetadata replayMetadata = app.getReplayMetadataAccessor().getReplayMetadata(replay);
        Path replayFileName = FileUtil.changeExtension(replayBackupPath.getFileName(), FILE_EXTENSION_REPLAY);
        Path liveReplayPath = getLiveReplayDirectory().resolve(replayFileName);
        return new ReplayEntry(liveReplayPath, replayBackupPath, replay, replayMetadata);
    }

    private boolean isInvalidPath(Path filePath, Path expectedDirectory, String expectedFileExtension) {
        return !Files.isRegularFile(filePath) || !FileUtil.getExtension(filePath).equals(expectedFileExtension) || !filePath.startsWith(expectedDirectory);
    }

    /**
     * Performs a complete refresh of all replay files on disk. In order, this method:
     * <ol>
     *     <li>Invokes {@link #getMismatchingReplayBackupPaths()}
     *     <li>Invokes {@link #createReplayBackups(Collection)} (with the result of the last step)
     *     <li>Invokes {@link #loadReplays(Collection)} (with the result of the last step)
     *     <li>Invokes {@link #loadReplays()}
     * </ol>
     * This method is useful on startup or for when the live replay directory is changed at runtime.
     *
     * @return all loaded {@link ReplayEntry} objects
     */
    public CompletionStage<Collection<ReplayEntry>> performCompleteRefresh() {
        return getMismatchingReplayBackupPaths()
                .thenCompose(this::createReplayBackups)
                .thenCompose(_ -> loadReplays()); // Be sure to load ALL replays in a complete refresh
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

    public CompletionStage<Void> deleteReplays(Collection<Path> liveReplayPaths, boolean fullDelete) {
        Path liveReplayDirectory = getLiveReplayDirectory();
        if (!Files.isDirectory(liveReplayDirectory) || liveReplayPaths.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.runAsync(() -> {
            Path recentlyDeletedDirectory = fileStructure.recentlyDeletedDirectory();
            FileUtil.createDirectoryIfDoesntExist(recentlyDeletedDirectory);

            Path replayBackupsDirectory = fileStructure.replayBackupsDirectory();

            for (Path path : liveReplayPaths) {
                // Ignore any invalid files (non-existent, non-replay files, or not in the live replay directory)
                if (isInvalidPath(path, liveReplayDirectory, FILE_EXTENSION_REPLAY)) {
                    continue;
                }

                try {
                    if (fullDelete) {
                        Files.copy(path, recentlyDeletedDirectory.resolve(path.getFileName()), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                        Files.deleteIfExists(replayBackupsDirectory.resolve(path.getFileName()));
                    }

                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    throw new CompletionException(e);
                }
            }
        }, app.getExecutor());
    }

}
