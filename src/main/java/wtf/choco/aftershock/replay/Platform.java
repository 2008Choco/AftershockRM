package wtf.choco.aftershock.replay;

import java.util.HashMap;
import java.util.Map;

public enum Platform {

    STEAM("OnlinePlatform_Steam"),
    PS4("OnlinePlatform_PS4"),
    XBOX("OnlinePlatform_Dingo"), // Dingo? lol...
    UNKNOWN("OnlinePlatform_Unknown");


    private static final Map<String, Platform> PLATFORMS = new HashMap<>(Platform.values().length, 1);

    static {
        for (Platform platform : Platform.values()) {
            PLATFORMS.put(platform.id, platform);
        }
    }

    private final String id;

    private Platform(String id) {
        this.id = id;
    }

    public static Platform getPlatform(String id) {
        return PLATFORMS.getOrDefault(id, UNKNOWN);
    }

}
