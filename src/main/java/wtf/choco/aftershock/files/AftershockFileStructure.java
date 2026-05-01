package wtf.choco.aftershock.files;

import java.nio.file.Path;

public final class AftershockFileStructure {

    // Directories
    private final Path installDirectory;
    private final Path replayBackupsDirectory;
    private final Path replayHeadersDirectory;

    // Files
    private final Path propertiesFile;
    private final Path binsFile;
    private final Path replayMetadataFile;

    public AftershockFileStructure(Path installDirectory) {
        // Directories
        this.installDirectory = installDirectory;
        this.replayBackupsDirectory = installDirectory.resolve("ReplayBackups/");
        this.replayHeadersDirectory = installDirectory.resolve("Headers/");

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
     * @return the directory where backups of all live replays are stored
     */
    public Path replayBackupsDirectory() {
        return replayBackupsDirectory;
    }

    /**
     * @return the directory where all JSON replay headers are stored
     */
    public Path replayHeadersDirectory() {
        return replayHeadersDirectory;
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
