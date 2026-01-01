package wtf.choco.aftershock.manager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import wtf.choco.aftershock.App;
import wtf.choco.aftershock.structure.ReplayBin;
import wtf.choco.aftershock.structure.ReplayEntry;
import wtf.choco.aftershock.util.JsonUtil;

import java.io.File;
import java.util.List;
import java.util.UUID;

public class BinRegistry  {

    public static final ReplayBin GLOBAL_BIN = new ReplayBin(UUID.randomUUID(), "Global", true);

    private final ObservableList<ReplayBin> bins = FXCollections.observableArrayList();

    public BinRegistry() {
        this.addBin(GLOBAL_BIN);
    }

    public ReplayBin createBin(String name) {
        if (name == null || name.equalsIgnoreCase("global")) {
            throw new IllegalStateException("'Global' is a reserved bin identifier");
        }

        for (ReplayBin bin : bins) {
            if (bin.getName().equalsIgnoreCase(name)) {
                return null;
            }
        }

        ReplayBin bin = new ReplayBin(UUID.randomUUID(), name);
        this.addBin(bin);
        return bin;
    }

    public void addBin(ReplayBin bin) {
        this.bins.add(bin);
    }

    public ReplayBin getBin(int index) {
        return bins.get(index);
    }

    public ReplayBin getBin(String name) {
        for (ReplayBin bin : bins) {
            if (bin.getName().equalsIgnoreCase(name)) {
                return bin;
            }
        }

        return null;
    }

    public void deleteBin(ReplayBin bin) {
        this.bins.remove(bin);
    }

    public void clearBins(boolean clearGlobal) {
        for (ReplayBin bin : bins) {
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

    public ObservableList<ReplayBin> getBins() {
        return bins;
    }

    public int getBinCount() {
        return bins.size();
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
            boolean hidden = JsonUtil.getOrCreate(binRoot, "hidden", JsonElement::getAsBoolean, JsonObject::addProperty, false);

            ReplayBin bin = new ReplayBin(uuid, name);
            bin.setHidden(hidden);

            JsonArray replays = binRoot.getAsJsonArray("replays");
            for (JsonElement replayIdElement : replays) {
                ReplayEntry replay = GLOBAL_BIN.getReplayById(replayIdElement.getAsString());
                if (replay == null) {
                    continue;
                }

                bin.addReplay(replay);
            }

            App.getInstance().getLogger().info("Loaded bin: \"" + name + "\"" + (hidden ? " (hidden)" : ""));
            this.addBin(bin);
        }
    }

    public void saveBinsToFile(File file) {
        JsonArray root = new JsonArray();

        for (ReplayBin bin : bins) {
            if (bin == GLOBAL_BIN) { // Don't write global bin to file
                continue;
            }

            JsonObject binRoot = new JsonObject();

            binRoot.addProperty("id", bin.getUUID().toString());
            binRoot.addProperty("name", bin.getName());
            binRoot.addProperty("hidden", bin.isHidden());

            List<ReplayEntry> replays = bin.getReplays();
            JsonArray replaysArray = new JsonArray(replays.size());
            for (ReplayEntry replay : replays) {
                replaysArray.add(replay.id());
            }

            binRoot.add("replays", replaysArray);
            root.add(binRoot);
        }

        JsonUtil.writeToFile(file, root);
    }

    public String getSafeName(String base) {
        int duplicateCount = 0;
        String result = base;

        do {
            result = (base + (duplicateCount++ >= 1 ? " (" + duplicateCount + ")" : ""));
        } while (getBin(result) != null);

        return result;
    }

}
