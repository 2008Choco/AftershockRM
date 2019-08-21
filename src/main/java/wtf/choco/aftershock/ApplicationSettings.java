package wtf.choco.aftershock;

public class ApplicationSettings {

    private String replayLocation, installDirectory, rattletrapPath;
    private String localeCode;

    public ApplicationSettings(String replayLocation, String installDirectory, String rattletrapPath, String localeCode) {
        this.replayLocation = replayLocation;
        this.installDirectory = installDirectory;
        this.rattletrapPath = rattletrapPath;
        this.localeCode = localeCode;
    }

    public void setReplayLocation(String replayLocation) {
        this.replayLocation = replayLocation;
    }

    public String getReplayLocation() {
        return replayLocation;
    }

    public void setInstallDirectory(String installDirectory) {
        this.installDirectory = installDirectory;
    }

    public String getInstallDirectory() {
        return installDirectory;
    }

    public void setRattletrapPath(String rattletrapPath) {
        this.rattletrapPath = rattletrapPath;
    }

    public String getRattletrapPath() {
        return rattletrapPath;
    }

    public void setLocaleCode(String localeCode) {
        this.localeCode = localeCode;
    }

    public String getLocaleCode() {
        return localeCode;
    }

}
