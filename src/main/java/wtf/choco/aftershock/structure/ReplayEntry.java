package wtf.choco.aftershock.structure;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.scene.Parent;
import wtf.choco.aftershock.App;
import wtf.choco.aftershock.controller.InfoPanelController;
import wtf.choco.aftershock.replay.AftershockData;
import wtf.choco.aftershock.replay.NewReplay;
import wtf.choco.aftershock.util.JsonUtil;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

public class ReplayEntry {

    private Parent infoPanel;

    private final BooleanProperty loaded;
    private final StringProperty comments;
    private final ListProperty<Tag> tags;

    private final File replayFile;
    private final File cachedReplayFile;
    private final File headerJsonFile;
    private final NewReplay replayData;

    public ReplayEntry(File replayFile, File cachedReplayFile, File headerJsonFile, NewReplay replayData) {
        this.replayFile = replayFile;
        this.cachedReplayFile = cachedReplayFile;
        this.headerJsonFile = headerJsonFile;
        this.replayData = replayData;

        AftershockData aftershockData = replayData.aftershockData();
        this.loaded = new SimpleBooleanProperty(aftershockData.isLoaded());
        this.comments = new SimpleStringProperty(aftershockData.getComments());
        this.tags = new SimpleListProperty<>(FXCollections.observableArrayList(aftershockData.getTags()));
    }

    public File getReplayFile() {
        return replayFile;
    }

    public File getCachedReplayFile() {
        return cachedReplayFile;
    }

    public File getHeaderJsonFile() {
        return headerJsonFile;
    }

    public NewReplay getReplayData() {
        return replayData;
    }

    public Parent getInfoPanel(ResourceBundle resources) {
        if (infoPanel == null) {
            this.infoPanel = InfoPanelController.createInfoPanelFor(replayData, resources);
        }

        return infoPanel;
    }

    public void setLoaded(boolean loaded) {
        this.loaded.set(loaded);
    }

    public boolean isLoaded() {
        return loaded.get();
    }

    public BooleanProperty loadedProperty() {
        return loaded;
    }

    public void setComments(String comments) {
        this.comments.set(comments);
    }

    public String getComments() {
        return comments.get();
    }

    public StringProperty commentsProperty() {
        return comments;
    }

    public void addTag(Tag tag) {
        this.tags.add(tag);
    }

    public void removeTag(Tag tag) {
        this.tags.remove(tag);
    }

    public void clearTags() {
        this.tags.clear();
    }

    public List<Tag> getTags() {
        return Collections.unmodifiableList(tags.get());
    }

    public ListProperty<Tag> tagsProperty() {
        return tags;
    }

    public void writeToHeader() {
        JsonObject root = null;
        try (FileReader reader = new FileReader(headerJsonFile)) {
            root = App.GSON.fromJson(reader, JsonObject.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (root == null) {
            throw new UnsupportedOperationException("Could not properly read JSON from header for file " + headerJsonFile.getAbsolutePath());
        }

        JsonObject aftershockRoot = JsonUtil.getOrCreate(root, "aftershock", JsonElement::getAsJsonObject, JsonObject::add, new JsonObject());
        aftershockRoot.addProperty("loaded", loaded.getValue());
        aftershockRoot.addProperty("comments", comments.getValueSafe());

        JsonArray tagsArray = new JsonArray();
        this.tags.forEach(tag -> tagsArray.add(tag.getUUID().toString()));
        aftershockRoot.add("tags", tagsArray);

        try (JsonWriter writer = App.GSON.newJsonWriter(new FileWriter(headerJsonFile))) {
            App.GSON.toJson(root, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void registerPropertyListeners(App app) {
        this.loaded.addListener((_, oldValue, newValue) -> {
            if (oldValue != newValue) {
                app.processReplayIO(this);
            }
        });

        this.comments.addListener((_, oldValue, newValue) -> {
            if (!Objects.equals(oldValue, newValue)) {
                app.processReplayIO(this);
            }
        });

        this.tags.addListener((ListChangeListener<Tag>) _ -> app.processReplayIO(this));
    }

}
