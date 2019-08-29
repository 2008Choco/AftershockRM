package wtf.choco.aftershock;

public class ApplicationSettings {

    private String replayLocation, rattletrapPath;
    private String locale;

    public ApplicationSettings(String replayLocation, String rattletrapPath, String localeCode) {
        this.replayLocation = replayLocation;
        this.rattletrapPath = rattletrapPath;
        this.locale = localeCode;
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

    public void setLocale(String localeCode) {
        this.locale = localeCode;
    }

    public String getLocale() {
        return locale;
    }

}
