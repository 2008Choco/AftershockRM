package wtf.choco.aftershock.manager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
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

    private final ListProperty<ReplayBin> bins = new SimpleListProperty<>(this, "bins", FXCollections.observableArrayList());
    private final ReplayBin globalBin;

    public BinRegistry() {
        this.globalBin = new ReplayBin(UUID.randomUUID(), "Global", true);
        this.addBin(globalBin);
    }

    public ReplayBin getGlobalBin() {
        return globalBin;
    }

    public ReplayBin createBin(String name) {
        if (name == null || name.equalsIgnoreCase("global")) {
            throw new IllegalStateException("'Global' is a reserved bin identifier");
        }

        for (ReplayBin bin : getBins()) {
            if (bin.getName().equalsIgnoreCase(name)) {
                return null;
            }
        }

        ReplayBin bin = new ReplayBin(UUID.randomUUID(), name);
        this.addBin(bin);
        return bin;
    }

    public void addBin(ReplayBin bin) {
        this.getBins().add(bin);
    }

    public ReplayBin getBin(int index) {
        return getBins().get(index);
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
        this.getBins().remove(bin);
    }

    public void clearBins(boolean includeGlobal) {
        for (ReplayBin bin : getBins()) {
            if (!includeGlobal && bin.isGlobal()) {
                continue;
            }

            bin.clear();
        }
    }

    public void deleteBins(boolean includeGlobal) {
        this.getBins().removeIf(bin -> includeGlobal || !bin.isGlobal());
    }

    public ObservableList<ReplayBin> getBins() {
        return bins.get();
    }

    public ListProperty<ReplayBin> binsProperty() {
        return bins;
    }

    public void loadBinsFromFile(File file) {
        // TODO: Write a TypeAdapter for this instead of parsing JSON on the fly
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
                ReplayEntry replay = globalBin.getReplay(replayIdElement.getAsString());
                if (replay == null) {
                    continue;
                }

                bin.getReplays().add(replay);
            }

            App.getInstance().getLogger().info("Loaded bin: \"" + name + "\"" + (hidden ? " (hidden)" : ""));

            // TODO: This needs to be done better by properly running things in bulk on the application thread
            Platform.runLater(() -> addBin(bin));
        }
    }

    public void saveBinsToFile(File file) {
        JsonArray root = new JsonArray();

        for (ReplayBin bin : getBins()) {
            if (bin.isGlobal()) { // Don't write global bin to file
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
