package wtf.choco.aftershock.replay.schema;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import wtf.choco.aftershock.replay.AftershockData;
import wtf.choco.aftershock.replay.Goal;
import wtf.choco.aftershock.replay.NewReplay;
import wtf.choco.aftershock.replay.Player;
import wtf.choco.aftershock.replay.Team;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class ReplayTypeAdapter extends TypeAdapter<NewReplay> {

    /*
     * It would be WAY more efficient to read these files directly from binary rather than having to run the replay files through
     * a third-party JSON deserialization program and streaming JSON data into an adapter, but this is a problem for later.
     * The replay format is notoriously undocumented by Psyonix, and while community efforts are appreciated, they're unreliable.
     * One way or another, I have to rely on a community solution...
     *
     * Maybe one day I'll read from binary. I think it's smarter and would increase the speed of Aftershock's parsing significantly.
     */

    private static final String NAME_AFTERSHOCK = "Aftershock";
    private static final String NAME_PROPERTIES = "Properties";
    private static final String NAME_REPLAY_ID = "Id";
    private static final String NAME_REPLAY_NAME = "ReplayName";
    private static final String NAME_PLAYER_NAME = "PlayerName";
    private static final String NAME_MAP_ID = "MapName";
    private static final String NAME_TEAM_SIZE = "TeamSize";
    private static final String NAME_TEAM_0_SCORE = "Team0Score";
    private static final String NAME_TEAM_1_SCORE = "Team1Score";
    private static final String NAME_DURATION = "NumFrames";
    private static final String NAME_FPS = "RecordFPS";
    private static final String NAME_DATE = "Date";
    private static final String NAME_PLAYERS = "PlayerStats";
    private static final String NAME_GOALS = "Goals";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("uuuu-MM-dd HH-mm-ss");

    private final Gson gson;

    public ReplayTypeAdapter(Gson gson) {
        this.gson = gson;
    }

    @Override
    public NewReplay read(JsonReader in) throws IOException {
        in.beginObject();

        AftershockData aftershockData = null;
        String replayId = "[UNKNOWN_REPLAY_ID]";
        String replayName = "[UNKNOWN_REPLAY_NAME]";
        String playerName = "[UNKNOWN_PLAYER]";
        String mapId = "[UNKNOWN_MAP]";
        int teamSize = 3;
        Map<Team, Integer> scores = new EnumMap<>(Team.class);
        int duration = 0;
        double framesPerSecond = 30.0;
        LocalDateTime date = LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC);
        List<Player> players = new ArrayList<>();
        List<Goal> goals = new ArrayList<>();

        while (in.hasNext()) {
            switch (in.nextName()) {
                case NAME_AFTERSHOCK -> aftershockData = gson.getAdapter(AftershockData.class).read(in);
                case NAME_PROPERTIES -> {
                    in.beginObject();

                    while (in.hasNext()) {
                        switch (in.nextName()) {
                            case NAME_REPLAY_ID -> replayId = in.nextString();
                            case NAME_REPLAY_NAME -> replayName = in.nextString();
                            case NAME_PLAYER_NAME -> playerName = in.nextString();
                            case NAME_MAP_ID -> {
                                // Map name is special because it's an object with a "Value" field, e.g. "MapName": { "Value": "the_map_id" }
                                in.beginObject();
                                while (in.hasNext()) {
                                    if (in.nextName().equals("Value")) {
                                        mapId = in.nextString();
                                    } else {
                                        in.skipValue();
                                    }
                                }
                                in.endObject();
                            }
                            case NAME_TEAM_SIZE -> teamSize = in.nextInt();
                            case NAME_TEAM_0_SCORE -> scores.put(Team.BLUE, in.nextInt());
                            case NAME_TEAM_1_SCORE -> scores.put(Team.ORANGE, in.nextInt());
                            case NAME_DURATION -> duration = in.nextInt();
                            case NAME_FPS -> framesPerSecond = in.nextDouble();
                            case NAME_DATE -> date = LocalDateTime.parse(in.nextString(), DATE_FORMATTER);
                            case NAME_PLAYERS -> {
                                in.beginArray();
                                while (in.hasNext()) {
                                    players.add(gson.getAdapter(Player.class).read(in));
                                }
                                in.endArray();
                            }
                            case NAME_GOALS -> {
                                in.beginArray();
                                while (in.hasNext()) {
                                    goals.add(gson.getAdapter(Goal.class).read(in));
                                }
                                in.endArray();
                            }
                            default -> in.skipValue();
                        }
                    }

                    in.endObject();
                }
                default -> in.skipValue();
            }
        }

        in.endObject();

        if (aftershockData == null) {
            aftershockData = new AftershockData();
        }

        return new NewReplay(replayId, replayName, playerName, mapId, teamSize, scores, duration, framesPerSecond, date, players, goals, aftershockData);
    }

    @Override
    public void write(JsonWriter out, NewReplay value) throws IOException {
        // NO-OP: We don't ever serialize this
    }

}
