package wtf.choco.aftershock.replay.schema;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import wtf.choco.aftershock.replay.Player;
import wtf.choco.aftershock.replay.Team;

import java.io.IOException;

public final class PlayerTypeAdapter extends TypeAdapter<Player> {

    private static final String NAME_NAME = "Name";
    private static final String NAME_TEAM = "Team";
    private static final String NAME_SCORE = "Score";
    private static final String NAME_GOALS = "Goals";
    private static final String NAME_ASSISTS = "Assists";
    private static final String NAME_SAVES = "Saves";
    private static final String NAME_SHOTS = "Shots";

    @Override
    public Player read(JsonReader in) throws IOException {
        in.beginObject();

        String name = "[UNKNOWN_PLAYER]";
        Team team = Team.BLUE;
        int score = 0;
        int goals = 0;
        int assists = 0;
        int saves = 0;
        int shots = 0;

        while (in.hasNext()) {
            String fieldName = in.nextName();
            switch (fieldName) {
                case NAME_NAME -> name = in.nextString();
                case NAME_TEAM -> team = Team.fromInternalId(in.nextInt());
                case NAME_SCORE -> score = in.nextInt();
                case NAME_GOALS -> goals = in.nextInt();
                case NAME_ASSISTS -> assists = in.nextInt();
                case NAME_SAVES -> saves = in.nextInt();
                case NAME_SHOTS -> shots = in.nextInt();
                default -> in.skipValue();
            }
        }

        in.endObject();
        return new Player(name, team, score, goals, assists, saves, shots);
    }

    @Override
    public void write(JsonWriter out, Player value) throws IOException {
        // NO-OP: We don't ever serialize this
    }

}
