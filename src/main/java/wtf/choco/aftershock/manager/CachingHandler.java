package wtf.choco.aftershock.manager;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import wtf.choco.aftershock.App;
import wtf.choco.aftershock.ApplicationSettings;
import wtf.choco.aftershock.replay.AftershockData;
import wtf.choco.aftershock.replay.IReplay;
import wtf.choco.aftershock.replay.Replay;
import wtf.choco.aftershock.structure.ReplayEntry;
import wtf.choco.aftershock.util.PublicTask;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class CachingHandler {

    private static final FilenameFilter REPLAY_FILE_FILTER = (_, name) -> name.endsWith(".replay");

    private MessageDigest md5;
    private Set<File> invalidatedReplays = Collections.emptySet();
    private Map<String, AftershockData> replayAftershockData = new HashMap<>();

    private final App app;
    private final File cacheDirectory, headersDirectory;

    public CachingHandler(App app) {
        this.app = app;
        this.cacheDirectory = new File(app.getInstallDirectory(), "Cache");
        this.headersDirectory = new File(app.getInstallDirectory(), "Headers");

        this.cacheDirectory.mkdirs();
        this.headersDirectory.mkdirs();

        try {
            this.md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            this.md5 = null;
            this.app.getLogger().severe("Could not find MD5 algorithm... replay files will not be updated!");
        }
    }

    public void cacheReplays(PublicTask<?> task) {
        Logger logger = app.getLogger();

        File replayDirectory = getReplayDirectory(logger);
        if (replayDirectory == null || !replayDirectory.exists() || !replayDirectory.isDirectory()) {
            logger.warning("Replay directory path does not exist or is not a directory. Could not cache");
            return;
        }

        long start = System.currentTimeMillis();

        File[] replayFiles = replayDirectory.listFiles(REPLAY_FILE_FILTER);
        if (replayFiles == null) {
            logger.warning("Replay directory path does not represent a directory or an IO exception occured. Could not cache");
            return;
        }

        int cached = 0, current = 0, expected = replayFiles.length;

        for (File replayFile : replayFiles) {
            String replayFileName = replayFile.getName();
            this.updateProgress(task, current++, expected, "Caching " + replayFileName.substring(0, replayFileName.lastIndexOf('.')) + "...");

            // Copy replay file to cache folder
            File cacheDestination = new File(cacheDirectory, replayFileName);
            boolean shouldCache = false;
            if (!cacheDestination.exists()) {
                logger.info("(" + App.truncateID(replayFileName) + ") - Caching replay file");
                shouldCache = true;
            } else if (md5 != null && !md5(replayFile).equals(md5(cacheDestination))) {
                logger.info("(" + App.truncateID(replayFileName) + ") - Replay updated. MD5 does not match. Re-caching");

                if (invalidatedReplays == Collections.EMPTY_SET) {
                    this.invalidatedReplays = new HashSet<>();
                }

                this.invalidatedReplays.add(cacheDestination);
                shouldCache = true;
            }

            if (shouldCache) {
                try {
                    Files.copy(replayFile.toPath(), cacheDestination.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    cached++;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        if (cached > 0) {
            long now = System.currentTimeMillis() - start;
            logger.info("Completed caching " + cached + " replays in " + now + "ms");
        } else {
            logger.info("No new replays were found. Caching not required");
        }
    }

    public void cacheReplays(PublicTask<?> task, Collection<File> replayFiles) {
        Logger logger = app.getLogger();

        long start = System.currentTimeMillis();
        int cached = 0, current = 0, expected = replayFiles.size();

        for (File replayFile : replayFiles) {
            String replayFileName = replayFile.getName();
            this.updateProgress(task, current++, expected, "Caching " + replayFileName.substring(0, replayFileName.lastIndexOf('.')) + "...");

            // Copy replay file to cache folder
            File cacheDestination = new File(cacheDirectory, replayFileName);
            boolean shouldCache = false;
            if (!cacheDestination.exists()) {
                logger.info("(" + App.truncateID(replayFileName) + ") - Caching replay file");
                shouldCache = true;
            } else if (md5 != null && !md5(replayFile).equals(md5(cacheDestination))) {
                logger.info("(" + App.truncateID(replayFileName) + ") - Replay updated. MD5 does not match. Re-caching");

                if (invalidatedReplays == Collections.EMPTY_SET) {
                    this.invalidatedReplays = new HashSet<>();
                }

                this.invalidatedReplays.add(cacheDestination);
                shouldCache = true;
            }

            if (shouldCache) {
                try {
                    Files.copy(replayFile.toPath(), cacheDestination.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    cached++;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        if (cached > 0) {
            long now = System.currentTimeMillis() - start;
            logger.info("Completed caching " + cached + " replays in " + now + "ms");
        } else {
            logger.info("No new replays were found. Caching not required");
        }
    }

    public void loadReplaysFromCache(PublicTask<?> task, boolean clearBins) throws IOException, JsonParseException {
        if (clearBins) {
            this.app.getBinRegistry().clearBins(true);
        }

        Logger logger = app.getLogger();

        File replayDirectory = getReplayDirectory(logger);
        if (replayDirectory == null || !replayDirectory.exists() || !replayDirectory.isDirectory()) {
            logger.warning("Replay directory path does not exist or is not a directory. Could not load replays");
            return;
        }

        long start = System.currentTimeMillis();

        File[] replayFiles = cacheDirectory.listFiles(REPLAY_FILE_FILTER);
        if (replayFiles == null) {
            logger.warning("Replay directory path does not represent a directory or an IO exception occured. Could not cache");
            return;
        }

        int loaded = 0, current = 0, expected = replayFiles.length;

        for (File cachedReplayFile : replayFiles) {
            String replayFileName = cachedReplayFile.getName();
            this.updateProgress(task, current++, expected, "Loading " + replayFileName.substring(0, replayFileName.lastIndexOf('.')) + "...");

            File replayFile = new File(replayDirectory, cachedReplayFile.getName());
            File headerFile = this.getOrCreateHeaderFile(logger, ApplicationSettings.ROCKETRP_PATH.get(), cachedReplayFile);

            Replay replayData = App.GSON.fromJson(new FileReader(headerFile), Replay.class);
            ReplayEntry replayEntry = new ReplayEntry(replayFile, cachedReplayFile, headerFile, replayData, replayAftershockData.computeIfAbsent(replayData.id(), _ -> new AftershockData()));

            // TODO: This listener registration needs to be moved elsewhere!
            replayEntry.loadedProperty().addListener((_, _, newValue) -> {
                if (newValue) {
                    try {
                        Files.copy(cachedReplayFile.toPath(), replayFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    replayFile.delete();
                }

                this.app.getController().updateLoadedLabel();
            });

            this.app.getBinRegistry().getGlobalBin().addReplay(replayEntry);
            loaded++;
        }

        long now = System.currentTimeMillis() - start;
        logger.info("Loaded " + loaded + " replays in " + now + "ms!");
    }

    public void loadReplaysFromCache(PublicTask<?> task) throws IOException, JsonParseException {
        this.loadReplaysFromCache(task, true);
    }

    public void loadReplays(PublicTask<?> task, Collection<File> replayFiles) throws IOException, JsonParseException {
        Logger logger = app.getLogger();

        long start = System.currentTimeMillis();

        int loaded = 0, current = 0, expected = replayFiles.size();

        for (File replayFile : replayFiles) {
            String replayFileName = replayFile.getName();
            this.updateProgress(task, current++, expected, "Loading " + replayFileName.substring(0, replayFileName.lastIndexOf('.')) + "...");

            File cachedReplayFile = new File(cacheDirectory, replayFile.getName());
            File headerFile = this.getOrCreateHeaderFile(logger, ApplicationSettings.ROCKETRP_PATH.get(), cachedReplayFile);

            Replay replayData = App.GSON.fromJson(new FileReader(headerFile), Replay.class);
            ReplayEntry replayEntry = new ReplayEntry(replayFile, cachedReplayFile, headerFile, replayData, replayAftershockData.computeIfAbsent(replayData.id(), _ -> new AftershockData()));

            // TODO: This listener registration needs to be moved elsewhere!
            replayEntry.loadedProperty().addListener((_, _, newValue) -> {
                if (newValue) {
                    try {
                        Files.copy(cachedReplayFile.toPath(), replayFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    replayFile.delete();
                }

                this.app.getController().updateLoadedLabel();
            });

            this.app.getBinRegistry().getGlobalBin().addReplay(replayEntry);
            loaded++;
        }

        long now = System.currentTimeMillis() - start;
        logger.info("Loaded " + loaded + " replays in " + now + "ms!");
    }

    private File getOrCreateHeaderFile(Logger logger, String rocketRPPath, File cachedReplayFile) {
        String replayFileName = cachedReplayFile.getName();
        File destination = new File(headersDirectory, replayFileName.substring(0, replayFileName.lastIndexOf('.')) + ".json");

        boolean shouldCreateHeader = false;
        if (!destination.exists()) {
            logger.info("(" + App.truncateID(replayFileName) + ") - Creating new header file");
            shouldCreateHeader = true;
        } else if (invalidatedReplays.contains(cachedReplayFile)) {
            logger.info("(" + App.truncateID(replayFileName) + ") - Updating existing header file");
            this.invalidatedReplays.remove(cachedReplayFile);
            shouldCreateHeader = true;
        }

        if (shouldCreateHeader) {
            try {
                new ProcessBuilder(
                    rocketRPPath,
                    "--fast",
                    "--replay", cachedReplayFile.getAbsolutePath(),
                    "--output", headersDirectory.getAbsolutePath()
                ).inheritIO().start().waitFor();
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }

            logger.info("Done!");
        }

        return destination;
    }

    private File getReplayDirectory(Logger logger) {
        String replayDirectoryPath = ApplicationSettings.REPLAY_DIRECTORY.get();
        if (replayDirectoryPath == null || replayDirectoryPath.isBlank()) {
            logger.warning("Missing replay directory path");
            return null;
        }

        return new File(replayDirectoryPath);
    }

    private String md5(File file) {
        try {
            return encodeHexString(md5.digest(Files.readAllBytes(file.toPath())));
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    private String encodeHexString(byte[] byteArray) {
        StringBuilder builder = new StringBuilder();

        for (byte b : byteArray) {
            builder.append(byteToHex(b));
        }

        return builder.toString();
    }

    private String byteToHex(byte num) {
        char[] hexDigits = new char[2];
        hexDigits[0] = Character.forDigit((num >> 4) & 0xF, 16);
        hexDigits[1] = Character.forDigit((num & 0xF), 16);

        return new String(hexDigits);
    }

    private void updateProgress(PublicTask<?> task, double done, double max, String message) {
        if (task == null) {
            return;
        }

        task.updateMessage(message);
        task.updateProgress(done, max);
    }

    public void loadReplayData(File replayDataFile) throws IOException, JsonParseException {
        this.replayAftershockData.clear();

        JsonObject root = App.GSON.fromJson(new FileReader(replayDataFile), JsonObject.class);
        if (root == null) {
            root = new JsonObject();
        }

        int loaded = 0;
        for (String replayID : root.keySet()) {
            AftershockData data = App.GSON.fromJson(root.getAsJsonObject(replayID), AftershockData.class);
            this.replayAftershockData.put(replayID, data);
            loaded++;
        }

        this.app.getLogger().info("Loaded replay data of " + loaded + " replays!");
    }

    public void writeReplayData(File replayDataFile) {
        JsonObject root = new JsonObject();
        for (String replayID : replayAftershockData.keySet()) {
            AftershockData data = replayAftershockData.get(replayID);
            root.add(replayID, App.GSON.toJsonTree(data));
        }

        try {
            Files.writeString(replayDataFile.toPath(), App.GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public AftershockData getAftershockData(IReplay replay) {
        return replayAftershockData.get(replay.id());
    }

}
