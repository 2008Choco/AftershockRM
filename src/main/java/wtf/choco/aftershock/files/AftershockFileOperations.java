package wtf.choco.aftershock.files;

import wtf.choco.aftershock.App;
import wtf.choco.aftershock.ApplicationSettings;
import wtf.choco.aftershock.replay.AftershockData;
import wtf.choco.aftershock.replay.Replay;
import wtf.choco.aftershock.structure.ReplayEntry;
import wtf.choco.aftershock.util.FileUtil;
import wtf.choco.aftershock.util.function.ThrowingFunction;
import wtf.choco.aftershock.util.function.ThrowingPredicate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.Predicate;
import java.util.stream.Stream;

public final class AftershockFileOperations {

    private static final String FILE_EXTENSION_REPLAY = "replay";
    private static final String FILE_EXTENSION_JSON = "json";
    private static final Predicate<Path> REPLAY_PATH_PREDICATE = path -> FileUtil.getExtension(path).equals(FILE_EXTENSION_REPLAY);
    private static final Predicate<Path> JSON_PATH_PREDICATE = path -> FileUtil.getExtension(path).equals(FILE_EXTENSION_JSON);

    private final MessageDigest md5;

    private final App app;
    private final Path replayBackupDirectory;
    private final Path headersDirectory;

    public AftershockFileOperations(App app) {
        this.app = app;
        this.replayBackupDirectory = app.getInstallPath().resolve("ReplayBackups/");
        this.headersDirectory = app.getInstallPath().resolve("Headers/");

        try {
            this.md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Couldn't get the MD5 hashing algorithm. How old is this computer?", e);
        }
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
     * Gets a {@link Path} to the directory holding backups of all live replay files. The contents of this
     * directory should mirror the files found in {@link #getLiveReplayDirectory()}.
     *
     * @return a path to the replay backup directory
     */
    public Path getReplayBackupDirectory() {
        return replayBackupDirectory;
    }

    /**
     * Get a {@link Path} to the directory holding the header files of all replay files. The headers stored
     * within this directory should correspond to the replay files found in {@link #getReplayBackupDirectory()}.
     *
     * @return a path to the headers directory
     */
    public Path getHeadersDirectory() {
        return headersDirectory;
    }

    public void createDirectoriesIfNotExist() {
        FileUtil.createDirectoryIfDoesntExist(getReplayBackupDirectory());
        FileUtil.createDirectoryIfDoesntExist(getHeadersDirectory());
    }

    /**
     * Gets a {@link Collection} of {@link Path Paths} to replay files in the {@link #getLiveReplayDirectory()
     * live replay directory} (limited to the provided paths) whose corresponding file in the {@link #getReplayBackupDirectory()
     * replay backup directory} do not match the signature of the live file, or do note xist in the replay backup
     * directory at all.
     *
     * @param liveReplayPaths the live replay paths to check
     *
     * @return a future that completes when all replays have been checked
     */
    public CompletionStage<Collection<Path>> getMismatchingReplayBackupPaths(Iterable<Path> liveReplayPaths) {
        return CompletableFuture.supplyAsync(() -> {
            List<Path> mismatchingPaths = new ArrayList<>();

            for (Path path : liveReplayPaths) {
                // Ignore any invalid files (non-existent, non-replay files, or not in the live replay directory)
                if (isInvalidPath(path, getLiveReplayDirectory(), FILE_EXTENSION_REPLAY)) {
                    continue;
                }

                try {
                    if (hasMismatchedBackupReplay(path)) {
                        mismatchingPaths.add(path);
                    }
                } catch (IOException _) { }
            }

            return mismatchingPaths;
        }, app.getExecutor());
    }

    /**
     * Gets a {@link Collection} of {@link Path Paths} to replay files in the {@link #getLiveReplayDirectory()
     * live replay directory} whose corresponding file in the {@link #getReplayBackupDirectory() replay backup
     * directory} do not match the signature of the live file, or do not exist in the replay backup directory at
     * all.
     * <p>
     * This method's stage result is most useful as a parameter to {@link #createReplayBackups(Iterable)}.
     *
     * @return a future that completes when all replays have been checked
     */
    public CompletionStage<Collection<Path>> getMismatchingReplayBackupPaths() {
        return CompletableFuture.supplyAsync(() -> {
            try (Stream<Path> paths = Files.list(getLiveReplayDirectory()).filter(REPLAY_PATH_PREDICATE)) {
                return paths.filter(ThrowingPredicate.unwrap(this::hasMismatchedBackupReplay)).toList();
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        }, app.getExecutor());
    }

    private boolean hasMismatchedBackupReplay(Path liveReplayPath) throws IOException {
        Path replayBackupPath = replayBackupDirectory.resolve(liveReplayPath.getFileName());
        if (Files.notExists(replayBackupPath)) {
            return true;
        }

        byte[] liveReplayHash = md5.digest(Files.readAllBytes(liveReplayPath));
        byte[] backupReplayHash = md5.digest(Files.readAllBytes(replayBackupPath));
        return !MessageDigest.isEqual(liveReplayHash, backupReplayHash);
    }

    /**
     * Creates backups for the given replay file paths in the {@link #getLiveReplayDirectory() live replay directory}
     * into the {@link #getReplayBackupDirectory() replay backup directory}. If a replay already exists in the replay
     * backup directory, it will be overwritten with the replay file from the live replay directory.
     *
     * @param liveReplayPaths the paths for which to create replay backup files. Any non-replay files or files that do
     * not reside in the live replay directory will be ignored
     *
     * @return a future that completes when the backup process has finished
     */
    public CompletionStage<Collection<Path>> createReplayBackups(Iterable<Path> liveReplayPaths) {
        return CompletableFuture.supplyAsync(() -> {
            List<Path> backedUpPaths = new ArrayList<>();

            Path liveReplayDirectory = getLiveReplayDirectory();
            for (Path path : liveReplayPaths) {
                // Ignore any invalid files (non-existent, non-replay files, or not in the live replay directory)
                if (isInvalidPath(path, liveReplayDirectory, FILE_EXTENSION_REPLAY)) {
                    continue;
                }

                Path targetPath = replayBackupDirectory.resolve(path.getFileName());
                boolean changed = Files.notExists(targetPath);
                try {
                    // If the target file existed before, we'll hash before and after to verify it's been changed
                    byte[] hash = null;
                    if (changed) {
                        hash = md5.digest(Files.readAllBytes(path));
                    }

                    // Actually perform the backup
                    Files.copy(path, targetPath, StandardCopyOption.REPLACE_EXISTING);

                    if (hash != null) {
                        changed = MessageDigest.isEqual(hash, md5.digest(Files.readAllBytes(path)));
                    }

                    if (changed) {
                        backedUpPaths.add(targetPath);
                    }
                } catch (IOException e) {
                    throw new CompletionException(e);
                }
            }

            return backedUpPaths;
        }, app.getExecutor());
    }

    /**
     * Creates backups for all replay files in the {@link #getLiveReplayDirectory() live replay directory} into
     * the {@link #getReplayBackupDirectory() replay backup directory}. If a replay already exists in the replay
     * backup directory, it will be overwritten with the replay from the live replay directory.
     *
     * @return a future that completes when the backup process has finished
     */
    public CompletionStage<Collection<Path>> createReplayBackups() {
        Path liveReplayDirectory = getLiveReplayDirectory();
        try (Stream<Path> paths = Files.list(liveReplayDirectory).filter(REPLAY_PATH_PREDICATE)) {
            return createReplayBackups(paths.toList());
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Generates header files for the given replay file paths in the {@link #getReplayBackupDirectory() replay backup
     * directory} and places them in the {@link #getHeadersDirectory() headers directory}. If a header already exists
     * in the headers directory, it will be overwritten.
     *
     * @param backupReplayPaths the paths for which to create replay headers. Any non-replay files or files that do not
     * reside in the replay backup directory will be ignored
     *
     * @return a future that completes when all header files have been generated
     */
    public CompletionStage<Collection<Path>> generateHeaders(Iterable<Path> backupReplayPaths) {
        String rocketRPPath = ApplicationSettings.ROCKETRP_PATH.get();
        return CompletableFuture.supplyAsync(() -> {
            List<Path> updatedHeaderPaths = new ArrayList<>();

            for (Path path : backupReplayPaths) {
                // Ignore any invalid files (non-existent, non-replay files, or not in the replay backup directory)
                if (isInvalidPath(path, replayBackupDirectory, FILE_EXTENSION_REPLAY)) {
                    continue;
                }

                Path targetPath = FileUtil.changeExtension(headersDirectory.resolve(path.getFileName()), FILE_EXTENSION_JSON);
                boolean changed = Files.notExists(targetPath);
                try {
                    // If the target file existed before, we'll hash before and after to verify it's been changed
                    byte[] hash = null;
                    if (changed) {
                        hash = md5.digest(Files.readAllBytes(path));
                    }

                    // Generate the header file using RocketRP
                    new ProcessBuilder(
                        rocketRPPath,
                        "--fast",
                        "--replay", path.toAbsolutePath().toString(),
                        "--output", headersDirectory.toAbsolutePath().toString()
                    ).inheritIO().start().waitFor();

                    if (hash != null) {
                        changed = MessageDigest.isEqual(hash, md5.digest(Files.readAllBytes(path)));
                    }

                    if (changed) {
                        updatedHeaderPaths.add(targetPath);
                    }
                } catch (IOException | InterruptedException e) {
                    throw new CompletionException(e);
                }
            }

            return updatedHeaderPaths;
        }, app.getExecutor());
    }

    /**
     * Generates header files for all replays in the {@link #getReplayBackupDirectory() replay backup directory}
     * and places them in the {@link #getHeadersDirectory() header directory}.
     *
     * @return a future that completes when all header files have been generated
     */
    public CompletionStage<Collection<Path>> generateHeaders() {
        try (Stream<Path> paths = Files.list(replayBackupDirectory).filter(REPLAY_PATH_PREDICATE)) {
            return generateHeaders(paths.toList());
        } catch (IOException e) {
            throw new CompletionException(e);
        }
    }

    /**
     * Deletes all header files from the {@link #getHeadersDirectory() header directory} that do not have
     * a corresponding replay in the {@link #getReplayBackupDirectory() replay backup directory}.
     *
     * @return a future that completes when the deletion of all invalid headers has finished
     */
    public CompletionStage<Void> deleteInvalidHeaders() {
        return CompletableFuture.runAsync(() -> {
            try (Stream<Path> paths = Files.list(headersDirectory).filter(REPLAY_PATH_PREDICATE)) {
                for (Iterator<Path> iterator = paths.iterator(); iterator.hasNext();) {
                    Path path = iterator.next();
                    if (!Files.exists(replayBackupDirectory.resolve(path.getFileName()))) {
                        Files.delete(path);
                    }
                }
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        }, app.getExecutor());
    }

    /**
     * Loads all header files from the given header file paths in the {@link #getHeadersDirectory() headers
     * directory}.
     *
     * @param headerPaths the paths of the header files to load. Any non-header files or files that do not reside in the
     * headers directory will be ignored
     *
     * @return a future that completes when all headers have been loaded
     */
    public CompletionStage<Collection<ReplayEntry>> loadHeaders(Iterable<Path> headerPaths) {
        return CompletableFuture.supplyAsync(() -> {
            System.out.println(headerPaths);

            List<ReplayEntry> replays = new ArrayList<>();
            for (Path path : headerPaths) {
                // Ignore any invalid files (non-existent, non-header files, or not in the headers directory)
                if (isInvalidPath(path, headersDirectory, FILE_EXTENSION_JSON)) {
                    System.out.println("INVALID HEADER PATH: " + path);
                    continue;
                }

                try {
                    replays.add(createReplayEntryFromHeader(path));
                } catch (IOException e) {
                    throw new CompletionException(e);
                }
            }
            System.out.println("RETURNING (" + replays.size() + ") REPLAY(S)!!!");
            return replays;
        }, app.getExecutor());
    }

    /**
     * Loads all header files in the {@link #getHeadersDirectory() headers directory}.
     *
     * @return a future that completes when all headers have been loaded
     */
    public CompletionStage<Collection<ReplayEntry>> loadHeaders() {
        return CompletableFuture.supplyAsync(() -> {
            try (Stream<Path> paths = Files.list(headersDirectory).filter(JSON_PATH_PREDICATE)) {
                return paths.map(ThrowingFunction.unwrap(this::createReplayEntryFromHeader)).toList();
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        }, app.getExecutor());
    }

    private ReplayEntry createReplayEntryFromHeader(Path headerPath) throws IOException {
        Replay replay = App.GSON.fromJson(Files.newBufferedReader(headerPath, StandardCharsets.UTF_8), Replay.class);
        AftershockData aftershockData = app.getCacheHandler().getAftershockData(replay); // TODO: Get from elsewhere, or rename getCacheHandler()... or something
        Path liveReplayPath = getLiveReplayDirectory().resolve(headerPath.getFileName().getName(0) + "." + FILE_EXTENSION_REPLAY);
        Path replayBackupPath = replayBackupDirectory.resolve(headerPath.getFileName().getName(0) + "." + FILE_EXTENSION_REPLAY);
        return new ReplayEntry(liveReplayPath, replayBackupPath, headerPath, replay, aftershockData);
    }

    private boolean isInvalidPath(Path path, Path expectedDirectory, String expectedFileExtension) {
        return Files.notExists(path) || !FileUtil.getExtension(path).equals(expectedFileExtension) || !path.startsWith(expectedDirectory);
    }

    /**
     * Performs a complete refresh of all replay files on disk. In order, this method:
     * <ol>
     *     <li>Invokes {@link #getMismatchingReplayBackupPaths()}
     *     <li>Invokes {@link #createReplayBackups(Iterable)} (with the result of the last step)
     *     <li>Invokes {@link #generateHeaders(Iterable)} (with the result of the last step)
     *     <li>Invokes {@link #loadHeaders()}
     *     <li>Invokes {@link #deleteInvalidHeaders()}
     * </ol>
     * This method is useful on startup or for when the live replay directory is changed at runtime.
     *
     * @return all loaded {@link ReplayEntry} objects
     */
    public CompletionStage<Collection<ReplayEntry>> performCompleteRefresh() {
        return getMismatchingReplayBackupPaths()
                .thenCompose(this::createReplayBackups)
                .thenCompose(this::generateHeaders)
                .thenCompose(_ -> loadHeaders()) // Be sure to load ALL headers in a complete refresh
                .thenCompose(result -> deleteInvalidHeaders().thenApply(_ -> result)); // Passing through the result of #loadHeaders()
    }

}
