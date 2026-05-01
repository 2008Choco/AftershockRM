package wtf.choco.aftershock.schema;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import wtf.choco.aftershock.App;
import wtf.choco.aftershock.replay.ReplayMetadata;
import wtf.choco.aftershock.structure.Tag;

import java.io.IOException;
import java.util.UUID;

public final class ReplayMetadataTypeAdapter extends TypeAdapter<ReplayMetadata> {

    private static final String NAME_LOADED = "Loaded";
    private static final String NAME_COMMENT = "Comment";
    private static final String NAME_TAGS = "Tags";

    @Override
    public ReplayMetadata read(JsonReader in) throws IOException {
        in.beginObject();

        ReplayMetadata object = new ReplayMetadata();

        while (in.hasNext()) {
            switch (in.nextName()) {
                case NAME_LOADED -> object.setLoaded(in.nextBoolean());
                case NAME_COMMENT -> object.setComment(in.nextString());
                case NAME_TAGS -> {
                    in.beginArray();
                    while (in.hasNext()) {
                        UUID tagUUID = UUID.fromString(in.nextString());
                        Tag tag = App.getInstance().getTagRegistry().getTag(tagUUID);
                        if (tag != null) {
                            object.addTag(tag);
                        }
                    }
                    in.endArray();
                }
                default -> in.skipValue();
            }
        }

        in.endObject();
        return object;
    }

    @Override
    public void write(JsonWriter out, ReplayMetadata value) throws IOException {
        out.beginObject();

        out.name(NAME_LOADED).value(value.isLoaded());
        out.name(NAME_COMMENT).value(value.getComment());
        out.name(NAME_TAGS);
        out.beginArray();
        for (Tag tag : value.getTags()) {
            out.value(tag.getUUID().toString());
        }
        out.endArray();

        out.endObject();
    }

}
