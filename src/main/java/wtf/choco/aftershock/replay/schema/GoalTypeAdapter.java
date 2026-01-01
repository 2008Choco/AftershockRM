package wtf.choco.aftershock.replay.schema;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import wtf.choco.aftershock.replay.Goal;
import wtf.choco.aftershock.replay.Team;

import java.io.IOException;

public final class GoalTypeAdapter extends TypeAdapter<Goal> {

    private static final String NAME_FRAME = "frame"; // The only lowercase key? lol
    private static final String NAME_PLAYER_NAME = "PlayerName";
    private static final String NAME_TEAM = "PlayerTeam";

    @Override
    public Goal read(JsonReader in) throws IOException {
        in.beginObject();

        int frame = 0;
        String playerName = "[UNKNOWN_PLAYER]";
        Team team = Team.BLUE;

        while (in.hasNext()) {
            String fieldName = in.nextName();
            switch (fieldName) {
                case NAME_FRAME -> frame = in.nextInt();
                case NAME_PLAYER_NAME -> playerName = in.nextString();
                case NAME_TEAM -> team = Team.fromInternalId(in.nextInt());
                default -> in.skipValue();
            }
        }

        in.endObject();
        return new Goal(frame, playerName, team);
    }

    @Override
    public void write(JsonWriter out, Goal value) throws IOException {
        // NO-OP: We don't ever serialize this
    }

}
