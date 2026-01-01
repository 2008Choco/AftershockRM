package wtf.choco.aftershock.replay;

import java.util.concurrent.TimeUnit;

public record Goal(int frame, String playerName, Team team) {

    public long timestamp(Replay replay, TimeUnit unit) {
        long millisecond = (long) Math.ceil((frame * 1000L) / replay.framesPerSecond());
        return (int) unit.convert(millisecond, TimeUnit.MILLISECONDS);
    }

}
