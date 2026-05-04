package wtf.choco.aftershock.files;

import java.nio.file.Path;

public final class AftershockFileStructure {

    // Directories
    private final Path installDirectory;
    private final Path unloadedReplayDirectory;
    private final Path recentlyDeletedDirectory;

    // Files
    private final Path propertiesFile;
    private final Path binsFile;
    private final Path replayMetadataFile;

    public AftershockFileStructure(Path installDirectory) {
        // Directories
        this.installDirectory = installDirectory;
        this.unloadedReplayDirectory = installDirectory.resolve("UnloadedReplays/");
        this.recentlyDeletedDirectory = installDirectory.resolve("RecentlyDeleted/");

        // Files
        this.propertiesFile = installDirectory.resolve("app.properties");
        this.binsFile = installDirectory.resolve("bins.json");
        this.replayMetadataFile = installDirectory.resolve("replay_metadata.json");
    }

    // Directories

    /**
     * @return the root installation directory
     */
    public Path installDirectory() {
        return installDirectory;
    }

    /**
     * @return the directory where unloaded replays are stored
     */
    public Path unloadedReplayDirectory() {
        return unloadedReplayDirectory;
    }

    /**
     * @return the directory where recently deleted replays are stored
     */
    public Path recentlyDeletedDirectory() {
        return recentlyDeletedDirectory;
    }

    // Files

    /**
     * @return the app's properties (settings) file
     */
    public Path propertiesFile() {
        return propertiesFile;
    }

    /**
     * @return the bin data file
     */
    public Path binsFile() {
        return binsFile;
    }

    /**
     * @return the replay metadata file
     */
    public Path replayMetadataFile() {
        return replayMetadataFile;
    }

}
