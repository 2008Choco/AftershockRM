package wtf.choco.aftershock.replay;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;

/**
 * Represents a read-only view of a replay file.
 */
public interface IReplay {

    /**
     * Get the replay's unique identifier, usually a series of random characters and numbers.
     *
     * @return the unique replay id
     */
    public String id();

    /**
     * Get the user-defined name of the replay shown in all user-facing UIs.
     *
     * @return the replay name
     */
    public String name();

    /**
     * Get the display name of the player that saved the replay.
     *
     * @return the player name
     */
    public String playerName();

    /**
     * Get the unique id of the map on which the replay was played.
     * <p>
     * Note that this is not the Aftershock translated name of the map, but the internal ID used by the game.
     *
     * @return the map id
     */
    public String mapId();

    /**
     * Get the translated name of the map (using the provided resources) on which the replay was played.
     *
     * @param resources the resource bundle containing translation strings
     *
     * @return the translated map name, or the translation key if no translation exists
     */
    public default String mapName(ResourceBundle resources) {
        String translationKey = "map.name." + mapId().toLowerCase();
        return resources.containsKey(translationKey) ? resources.getString(translationKey) : translationKey;
    }

    /**
     * Get the amount of players on each team.
     *
     * @return the team size
     */
    public int teamSize();

    /**
     * Get the score line of this replay as a mapping of teams to goals scored.
     *
     * @return the score line
     */
    public Map<Team, Integer> score();

    /**
     * Get the score line of this replay for a specific team.
     *
     * @param team the team whose score to get
     *
     * @return the team's score
     */
    public int score(Team team);

    /**
     * Get the duration of this replay calculated in frames. For a more user-friendly unit, use {@link #duration(TimeUnit)}
     * instead to convert to any unit of time.
     *
     * @return the duration of this replay in frames
     *
     * @see #framesPerSecond()
     */
    public int duration();

    /**
     * Get the duration of this replay converted to the specified time unit.
     *
     * @param unit the unit of time to convert the duration to
     *
     * @return the duration of this replay in the specified time unit
     */
    public int duration(TimeUnit unit);

    /**
     * Get the amount of frames saved to this replay per second. This metric can be used to calculate the real-time duration
     * of this replay in conjunction with {@link #duration()}.
     * <p>
     * Generally speaking this metric is not particularly useful and {@link #duration(TimeUnit)} should be used instead.
     *
     * @return the frames per second of this replay
     */
    public double framesPerSecond();

    /**
     * Get the {@link LocalDateTime date} that this replay was saved.
     *
     * @return the date the replay was saved
     */
    public LocalDateTime date();

    /**
     * Get a {@link List} of all {@link Player Players} that exist in this replay.
     *
     * @return the players
     */
    public List<Player> players();

    /**
     * Get a specific {@link Player} by name who participated in this replay.
     * <p>
     * This is a convenience method.
     *
     * @param name the (case-insensitive) name of the player to get
     *
     * @return the Player that participated in the replay, or null if none
     */
    public default Player player(String name) {
        return players().stream()
            .filter(player -> player.name().equalsIgnoreCase(name))
            .findFirst()
            .orElse(null);
    }

    /**
     * Get the {@link Goal Goals} that were scored in this replay.
     *
     * @return the goals
     */
    public List<Goal> goals();

}
