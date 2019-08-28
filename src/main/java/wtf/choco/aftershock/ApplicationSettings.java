package wtf.choco.aftershock;

public class ApplicationSettings {

    private String replayLocation, rattletrapPath;
    private String localeCode;

    public ApplicationSettings(String replayLocation, String rattletrapPath, String localeCode) {
        this.replayLocation = replayLocation;
        this.rattletrapPath = rattletrapPath;
        this.localeCode = localeCode;
    }

    public void setReplayLocation(String replayLocation) {
        this.replayLocation = replayLocation;
    }

    public String getReplayLocation() {
        return replayLocation;
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
