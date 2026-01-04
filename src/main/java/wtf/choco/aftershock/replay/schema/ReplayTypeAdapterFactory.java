package wtf.choco.aftershock.replay.schema;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import wtf.choco.aftershock.replay.AftershockData;
import wtf.choco.aftershock.replay.Goal;
import wtf.choco.aftershock.replay.Player;
import wtf.choco.aftershock.replay.Replay;

public class ReplayTypeAdapterFactory implements TypeAdapterFactory {

    @SuppressWarnings("unchecked")
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (Replay.class.isAssignableFrom(type.getRawType())) {
            return (TypeAdapter<T>) new ReplayTypeAdapter(gson);
        } else if (AftershockData.class.isAssignableFrom(type.getRawType())) {
            return (TypeAdapter<T>) new AftershockDataTypeAdapter();
        } else if (Player.class.isAssignableFrom(type.getRawType())) {
            return (TypeAdapter<T>) new PlayerTypeAdapter();
        } else if (Goal.class.isAssignableFrom(type.getRawType())) {
            return (TypeAdapter<T>) new GoalTypeAdapter();
        }

        return null;
    }

}
