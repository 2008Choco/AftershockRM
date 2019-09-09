package wtf.choco.aftershock.manager;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

import wtf.choco.aftershock.App;
import wtf.choco.aftershock.ApplicationSettings;
import wtf.choco.aftershock.replay.ReplayModifiable;
import wtf.choco.aftershock.structure.ReplayBin;

import javafx.beans.value.ChangeListener;

public class CachingHandler {

    private static final FilenameFilter REPLAY_FILE_FILTER = (f, name) -> name.endsWith(".replay");

    private final App app;
    private final File cacheDirectory, headersDirectory;

    public CachingHandler(App app) {
        this.app = app;
        this.cacheDirectory = new File(app.getInstallDirectory(), "Cache");
        this.headersDirectory = new File(app.getInstallDirectory(), "Headers");
    }

    public void cacheReplays() {
        Logger logger = app.getLogger();
        ApplicationSettings settings = app.getSettings();

        File replayDirectory = getReplayDirectory(logger, settings);
        if (!replayDirectory.exists() || !replayDirectory.isDirectory()) {
            logger.warning("Replay directory path does not exist or is not a directory. Could not cache");
            return;
        }

        long start = System.currentTimeMillis();
        int cached = 0;

        File[] replayFiles = replayDirectory.listFiles(REPLAY_FILE_FILTER);
        for (File replayFile : replayFiles) {
            String replayFileName = replayFile.getName();

            // Copy replay file to cache folder
            File cacheDestination = new File(cacheDirectory, replayFileName);
            if (!cacheDestination.exists()) {
                logger.info("(" + App.truncateID(replayFileName) + ") - Caching replay file");

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
            logger.info("Completed caching in " + now + "ms");
        } else {
            logger.info("No new replays were found. Caching not required");
        }
    }

    @Deprecated(forRemoval = false) // DANGEROUS AND DESTRUCTIVE METHOD
    public void invalidateAndDeleteCache() {
        for (File file : cacheDirectory.listFiles()) {
            file.delete();
        }

        for (File file : headersDirectory.listFiles()) {
            file.delete();
        }
    }

    public void loadReplaysFromCache(boolean clearBins) {
        if (clearBins) {
            this.app.getBinRegistry().clearBins(true);
            BinRegistry.GLOBAL_BIN.clear();
        }

        Logger logger = app.getLogger();
        ApplicationSettings settings = app.getSettings();

        File replayDirectory = getReplayDirectory(logger, settings);
        if (!replayDirectory.exists() || !replayDirectory.isDirectory()) {
            logger.warning("Replay directory path does not exist or is not a directory. Could not load replays");
            return;
        }

        long start = System.currentTimeMillis();

        File[] replayFiles = cacheDirectory.listFiles(REPLAY_FILE_FILTER);
        this.app.getController().prepareLoading(replayFiles.length);

        ReplayBin testBin = app.getBinRegistry().getBin("Testing Bin");
        for (File cachedReplayFile : cacheDirectory.listFiles(REPLAY_FILE_FILTER)) {
            File replayFile = new File(replayDirectory, cachedReplayFile.getName());
            File headerFile = this.getOrCreateHeaderFile(logger, settings.get(ApplicationSettings.RATTLETRAP_PATH), cachedReplayFile);

            ReplayModifiable replay = new ReplayModifiable(replayFile, cachedReplayFile, headerFile);
            replay.loadDataFromFile();

            replay.getEntryData().loadedProperty().addListener((ChangeListener<Boolean>) (change, oldValue, newValue) -> {
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

            this.app.getController().increaseLoadedReplay(1);
            BinRegistry.GLOBAL_BIN.addReplay(replay);

            if (ThreadLocalRandom.current().nextBoolean() && testBin.size() < 10) {
                testBin.addReplay(replay);
            }
        }

        long now = System.currentTimeMillis() - start;
        logger.info("Loaded " + BinRegistry.GLOBAL_BIN.size() + " replays in " + now + "ms!");
    }

    public void loadReplaysFromCache() {
        this.loadReplaysFromCache(true);
    }

    private File getOrCreateHeaderFile(Logger logger, String rattletrapPath, File replayFile) {
        String replayFileName = replayFile.getName();
        File destination = new File(headersDirectory, replayFileName.substring(0, replayFileName.lastIndexOf('.')) + ".json");

        if (!destination.exists()) {
            logger.info("(" + App.truncateID(replayFileName) + ") - Creating header file");

            try {
                Runtime.getRuntime().exec(rattletrapPath + " --f --i \"" + replayFile.getAbsolutePath() + "\" --o \"" + destination.getAbsolutePath() + "\"").waitFor();
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }

            logger.info("Done!");
        }

        return destination;
    }

    private File getReplayDirectory(Logger logger, ApplicationSettings settings) {
        String replayDirectoryPath = settings.get(ApplicationSettings.REPLAY_DIRECTORY);
        if (replayDirectoryPath == null || replayDirectoryPath.isBlank()) {
            logger.warning("Missing replay directory path");
            return null;
        }

        return new File(replayDirectoryPath);
    }

}
