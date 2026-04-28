package wtf.choco.aftershock.manager;

import com.google.gson.JsonObject;
import wtf.choco.aftershock.App;
import wtf.choco.aftershock.replay.AftershockData;
import wtf.choco.aftershock.replay.IReplay;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

public class CachingHandler { // TODO: This needs to be something different. It only holds references to AftershockData now

    private Map<String, AftershockData> replayAftershockData = new HashMap<>();

    private final App app;

    public CachingHandler(App app) {
        this.app = app;
    }

    public CompletionStage<Integer> loadReplayData(Path replayDataPath) {
        this.replayAftershockData.clear();
        return CompletableFuture.supplyAsync(() -> {
            int loaded = 0;
            try (BufferedReader reader = Files.newBufferedReader(replayDataPath, StandardCharsets.UTF_8)) {
                JsonObject root = App.GSON.fromJson(reader, JsonObject.class);
                if (root == null) {
                    root = new JsonObject();
                }

                for (String replayID : root.keySet()) {
                    AftershockData data = App.GSON.fromJson(root.getAsJsonObject(replayID), AftershockData.class);
                    this.replayAftershockData.put(replayID, data);
                    loaded++;
                }
            } catch (IOException e) {
                throw new CompletionException(e);
            }
            return loaded;
        }, app.getExecutor());
    }

    public void writeReplayData(Path replayDataPath) {
        JsonObject root = new JsonObject();
        for (String replayID : replayAftershockData.keySet()) {
            AftershockData data = replayAftershockData.get(replayID);
            root.add(replayID, App.GSON.toJsonTree(data));
        }

        try {
            Files.writeString(replayDataPath, App.GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public AftershockData getAftershockData(IReplay replay) {
        return replayAftershockData.computeIfAbsent(replay.id(), _ -> new AftershockData());
    }

}
