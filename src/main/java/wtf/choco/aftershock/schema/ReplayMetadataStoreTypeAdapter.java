package wtf.choco.aftershock.schema;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import wtf.choco.aftershock.files.ReplayMetadataStore;
import wtf.choco.aftershock.files.ReplayMetadataAccessor.ReplayMetadataEntry;
import wtf.choco.aftershock.replay.ReplayMetadata;

import java.io.IOException;

public final class ReplayMetadataStoreTypeAdapter extends TypeAdapter<ReplayMetadataStore> {

    private final Gson gson;

    ReplayMetadataStoreTypeAdapter(Gson gson) {
        this.gson = gson;
    }

    @Override
    public ReplayMetadataStore read(JsonReader in) throws IOException {
        in.beginObject();

        ReplayMetadataStore object = new ReplayMetadataStore();
        while (in.hasNext()) {
            String replayId = in.nextName();
            ReplayMetadata replayMetadata = gson.getAdapter(ReplayMetadata.class).read(in);
            object.setReplayMetadata(replayId, replayMetadata);
        }

        in.endObject();
        return object;
    }

    @Override
    public void write(JsonWriter out, ReplayMetadataStore value) throws IOException {
        out.beginObject();

        for (ReplayMetadataEntry entry : value.getAllReplayMetadata()) {
            out.name(entry.replayId());
            this.gson.getAdapter(ReplayMetadata.class).write(out, entry.metadata());
        }

        out.endObject();
    }

}
