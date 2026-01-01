package wtf.choco.aftershock.replay;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public record Replay(
        String id,
        String name,
        String playerName,
        String mapId,
        int teamSize,
        Map<Team, Integer> score,
        int duration,
        double framesPerSecond,
        LocalDateTime date,
        List<Player> players,
        List<Goal> goals
) implements IReplay {

    public Replay {
        // Make sure collection types are actually immutable
        score = Map.copyOf(score);
        players = List.copyOf(players);
        goals = List.copyOf(goals);
    }

    @Override
    public int score(Team team) {
        return score.getOrDefault(team, 0);
    }

    @Override
    public int duration(TimeUnit unit) {
        long durationInMillis = (int) Math.ceil((duration * 1000L) / framesPerSecond);
        return (int) unit.convert(durationInMillis, TimeUnit.MILLISECONDS);
    }

}
