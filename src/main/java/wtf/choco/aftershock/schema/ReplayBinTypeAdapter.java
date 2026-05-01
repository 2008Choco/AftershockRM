package wtf.choco.aftershock.schema;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import wtf.choco.aftershock.replay.IReplay;
import wtf.choco.aftershock.structure.ReplayBin;
import wtf.choco.aftershock.structure.ReplayEntry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

public final class ReplayBinTypeAdapter extends TypeAdapter<ReplayBin> {

    private static final String NAME_ID = "Id";
    private static final String NAME_NAME = "Name";
    private static final String NAME_HIDDEN = "Hidden";
    private static final String NAME_REPLAY_IDS = "ReplayIds";

    @Override
    public ReplayBin read(JsonReader in) throws IOException {
        in.beginObject();

        UUID id = null;
        String name = "Unnamed Bin";
        boolean hidden = false;
        Collection<ReplayEntry> replays = new ArrayList<>();

        while (in.hasNext()) {
            switch (in.nextName()) {
                case NAME_ID -> id = UUID.fromString(in.nextString());
                case NAME_NAME -> name = in.nextString();
                case NAME_HIDDEN -> hidden = in.nextBoolean();
                case NAME_REPLAY_IDS -> {
                    in.beginArray();
                    while (in.hasNext()) {
                        ReplayEntry replay = ReplayBin.GLOBAL.getReplay(in.nextString()); // TODO: I don't like this. This shouldn't rely on app state!
                        if (replay != null) {
                            replays.add(replay);
                        }
                    }
                    in.endArray();
                }
            }
        }

        in.endObject();
        return new ReplayBin(id, name, hidden, replays);
    }

    @Override
    public void write(JsonWriter out, ReplayBin value) throws IOException {
        out.beginObject();

        out.name(NAME_ID).value(value.getUUID().toString());
        out.name(NAME_NAME).value(value.getName());
        out.name(NAME_HIDDEN).value(value.isHidden());
        out.name(NAME_REPLAY_IDS);
        out.beginArray();
        for (IReplay replay : value.getReplays()) {
            out.value(replay.id());
        }
        out.endArray();

        out.endObject();
    }

}
