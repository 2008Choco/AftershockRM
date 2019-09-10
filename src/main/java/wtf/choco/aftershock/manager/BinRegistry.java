package wtf.choco.aftershock.manager;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import wtf.choco.aftershock.App;
import wtf.choco.aftershock.replay.Replay;
import wtf.choco.aftershock.structure.ReplayBin;
import wtf.choco.aftershock.structure.ReplayEntry;
import wtf.choco.aftershock.util.JsonUtil;

import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

public class BinRegistry  {

    public static final ReplayBin GLOBAL_BIN = new ReplayBin(UUID.randomUUID(), "Global");

    private final ObservableMap<String, ReplayBin> bins = FXCollections.observableHashMap();

    public BinRegistry() {
        this.addBin(GLOBAL_BIN);
    }

    public ReplayBin createBin(String name) {
        if (name == null || name.toLowerCase().equals("global")) {
            throw new IllegalStateException("'Global' is a reserved bin identifier");
        }

        ReplayBin bin = new ReplayBin(UUID.randomUUID(), name);
        this.bins.put(name.toLowerCase(), bin);
        return bin;
    }

    public void addBin(ReplayBin bin) {
        this.bins.put(bin.getName().toLowerCase(), bin);
    }

    public ReplayBin getBin(String name) {
        return bins.get(name.toLowerCase());
    }

    public void deleteBin(String name) {
        this.bins.remove(name.toLowerCase());
    }

    public void deleteBin(ReplayBin bin) {
        this.deleteBin(bin.getName().toLowerCase());
    }

    public void clearBins(boolean clearGlobal) {
        for (ReplayBin bin : bins.values()) {
            if (!clearGlobal && bin == GLOBAL_BIN) {
                continue;
            }

            bin.clear();
        }
    }

    public void deleteBins(boolean deleteGlobal) {
        this.clearBins(deleteGlobal);
        this.bins.clear();

        if (!deleteGlobal) {
            this.addBin(GLOBAL_BIN);
        }
    }

    public Collection<ReplayBin> getBins() {
        return Collections.unmodifiableCollection(bins.values());
    }

    public ObservableMap<String, ReplayBin> getObservableBins() {
        return bins;
    }

    public void loadBinsFromFile(File file, boolean deleteBins) {
        if (deleteBins) {
            this.deleteBins(false);
        }

        JsonArray root = JsonUtil.loadFromFile(file, JsonArray.class);
        if (root == null) {
            return;
        }

        for (JsonElement binElement : root) {
            if (!binElement.isJsonObject()) {
                continue;
            }

            JsonObject binRoot = binElement.getAsJsonObject();
            UUID uuid = UUID.fromString(binRoot.get("id").getAsString());
            String name = binRoot.get("name").getAsString();

            ReplayBin bin = new ReplayBin(uuid, name);

            JsonArray replays = binRoot.getAsJsonArray("replays");
            for (JsonElement replayIdElement : replays) {
                Replay replay = GLOBAL_BIN.getReplayById(replayIdElement.getAsString());
                if (replay == null) {
                    continue;
                }

                bin.addReplay(replay);
            }

            App.getInstance().getLogger().info("Loaded bin: \"" + name + "\"");
            this.addBin(bin);
        }
    }

    public void saveBinsToFile(File file) {
        JsonArray root = new JsonArray();

        for (ReplayBin bin : bins.values()) {
            if (bin == GLOBAL_BIN) { // Don't write global bin to file
                continue;
            }

            JsonObject binRoot = new JsonObject();

            binRoot.addProperty("id", bin.getUUID().toString());
            binRoot.addProperty("name", bin.getName());

            List<ReplayEntry> replays = bin.getReplays();
            JsonArray replaysArray = new JsonArray(replays.size());
            for (ReplayEntry replay : replays) {
                replaysArray.add(replay.getReplay().getId());
            }

            binRoot.add("replays", replaysArray);
            root.add(binRoot);
        }

        JsonUtil.writeToFile(file, root);
    }

}
