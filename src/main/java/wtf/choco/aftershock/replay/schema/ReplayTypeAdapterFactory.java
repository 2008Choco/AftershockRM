package wtf.choco.aftershock.replay.schema;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import wtf.choco.aftershock.files.ReplayMetadataAccessor;
import wtf.choco.aftershock.files.ReplayMetadataStore;
import wtf.choco.aftershock.replay.IReplay;
import wtf.choco.aftershock.replay.ReplayMetadata;
import wtf.choco.aftershock.replay.Goal;
import wtf.choco.aftershock.replay.Player;
import wtf.choco.aftershock.replay.Replay;

public class ReplayTypeAdapterFactory implements TypeAdapterFactory {

    @SuppressWarnings("unchecked")
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (IReplay.class.isAssignableFrom(type.getRawType())) {
            return (TypeAdapter<T>) new ReplayTypeAdapter(gson);
        } else if (Player.class.isAssignableFrom(type.getRawType())) {
            return (TypeAdapter<T>) new PlayerTypeAdapter();
        } else if (Goal.class.isAssignableFrom(type.getRawType())) {
            return (TypeAdapter<T>) new GoalTypeAdapter();
        } else if (ReplayMetadata.class.isAssignableFrom(type.getRawType())) {
            return (TypeAdapter<T>) new ReplayMetadataTypeAdapter();
        } else if (ReplayMetadataAccessor.class.isAssignableFrom(type.getRawType())) {
            return (TypeAdapter<T>) new ReplayMetadataStoreTypeAdapter(gson);
        }

        return null;
    }

}
