package wtf.choco.aftershock.replay;

import wtf.choco.rljp.structures.ReplayHeader;
import wtf.choco.rljp.structures.properties.ArrayProperty;
import wtf.choco.rljp.structures.properties.FloatProperty;
import wtf.choco.rljp.structures.properties.IntegerProperty;
import wtf.choco.rljp.structures.properties.PropertyList;
import wtf.choco.rljp.structures.properties.StringProperty;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object object) {
        // records generate hashCode() and equals() implementations for all fields. We only care about id()!
        return object == this || (object instanceof IReplay replay && id().equals(replay.id()));
    }

    private static final DateTimeFormatter DATE_PROPERTY_FORMAT = DateTimeFormatter.ofPattern("uuuu-MM-dd HH-mm-ss");

    public static Replay fromRLJPReplayHeader(ReplayHeader header) {
        PropertyList properties = header.properties();

        String replayId = getStringProperty(properties, "Id", "[UNKNOWN_REPLAY_ID]");
        String replayName = getStringProperty(properties, "ReplayName", "[UNKNOWN_REPLAY_NAME]");
        String playerName = getStringProperty(properties, "PlayerName", "[UNKNOWN_PLAYER]");
        String mapId = getStringProperty(properties, "MapName", "[UNKNOWN_MAP]");
        int teamSize = getIntProperty(properties, "TeamSize", 3);
        int duration = getIntProperty(properties, "NumFrames", 0);
        double framesPerSecond = getFloatProperty(properties, "RecordFPS", 30.0F);
        LocalDateTime date = LocalDateTime.parse(getStringProperty(properties, "Date", "1970-01-01 00-00-00"), DATE_PROPERTY_FORMAT);
        Map<Team, Integer> scores = Map.of(
            Team.BLUE, getIntProperty(properties, "Team0Score", 0),
            Team.ORANGE, getIntProperty(properties, "Team1Score", 0)
        );

        List<Player> players = properties.tryProperty("PlayerStats", ArrayProperty.class).stream().flatMap(array -> array.getProperties().stream()).map(playerProperties -> {
            String name = getStringProperty(playerProperties, "Name", "[UNKNOWN_PLAYER]");
            Team team = Team.fromInternalId(getIntProperty(playerProperties, "Team", 0));
            int score = getIntProperty(playerProperties, "Score", 0);
            int goals = getIntProperty(playerProperties, "Goals", 0);
            int assists = getIntProperty(playerProperties, "Assists", 0);
            int saves = getIntProperty(playerProperties, "Saves", 0);
            int shots = getIntProperty(playerProperties, "Shots", 0);
            return new Player(name, team, score, goals, assists, saves, shots);
        }).toList();

        List<Goal> goals = properties.tryProperty("Goals", ArrayProperty.class).stream().flatMap(array -> array.getProperties().stream()).map(goalProperties -> {
            int frame = getIntProperty(goalProperties, "frame", 0);
            String scorerName = getStringProperty(goalProperties, "PlayerName", "[UNKNOWN_PLAYER]");
            Team team = Team.fromInternalId(getIntProperty(goalProperties, "PlayerTeam", 0));
            return new Goal(frame, scorerName, team);
        }).toList();

        return new Replay(replayId, replayName, playerName, mapId, teamSize, scores, duration, framesPerSecond, date, players, goals);
    }

    private static String getStringProperty(PropertyList properties, String propertyName, String defaultValue) {
        return properties.tryProperty(propertyName, StringProperty.class).map(StringProperty::getValue).orElse(defaultValue);
    }

    private static int getIntProperty(PropertyList properties, String propertyName, int defaultValue) {
        IntegerProperty property = properties.property(propertyName, IntegerProperty.class);
        return (property != null) ? property.getValue() : defaultValue;
    }

    private static float getFloatProperty(PropertyList properties, String propertyName, float defaultValue) {
        FloatProperty property = properties.property(propertyName, FloatProperty.class);
        return (property != null) ? property.getValue() : defaultValue;
    }

}
