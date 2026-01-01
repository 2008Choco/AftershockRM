package wtf.choco.aftershock.replay;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @param id the id of the replay (usually a series of characters and numbers)
 * @param name the replay's user-defined name
 * @param playerName the name of the player that saved the replay
 * @param mapId the internal id of the map
 * @param teamSize the amount of players on each team
 * @param scores the scores for each team
 * @param duration the duration of the replay calculated in frames
 * @param framesPerSecond the frames per second used by the replay
 * @param date the date the replay was saved
 * @param players the players that participated in the replay
 * @param goals the goals that were scored in the replay
 * @param aftershockData data used specifically by Aftershock, not native to the replay format
 */
public record NewReplay(
        String id,
        String name,
        String playerName,
        String mapId,
        int teamSize,
        Map<Team, Integer> scores,
        int duration,
        double framesPerSecond,
        LocalDateTime date,
        List<Player> players,
        List<Goal> goals,
        AftershockData aftershockData
) {

    public NewReplay {
        // Make sure collection types are actually immutable
        scores = Map.copyOf(scores);
        players = List.copyOf(players);
        goals = List.copyOf(goals);
    }

    public int score(Team team) {
        return scores.getOrDefault(team, 0);
    }

    public int duration(TimeUnit unit) {
        long durationInMillis = (int) Math.ceil((duration * 1000L) / framesPerSecond);
        return (int) unit.convert(durationInMillis, TimeUnit.MILLISECONDS);
    }

    public Player player(String name) {
        return players.stream()
            .filter(player -> player.name().equalsIgnoreCase(name))
            .findFirst()
            .orElse(null);
    }

}
