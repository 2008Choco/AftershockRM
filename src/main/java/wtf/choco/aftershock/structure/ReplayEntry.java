package wtf.choco.aftershock.structure;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;

import wtf.choco.aftershock.App;
import wtf.choco.aftershock.replay.Replay;
import wtf.choco.aftershock.util.JsonUtil;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;

public class ReplayEntry {

    private final Replay replay;

    private BooleanProperty loaded = new SimpleBooleanProperty(true);
    private ListProperty<String> comments = new SimpleListProperty<>(FXCollections.observableArrayList());

    public ReplayEntry(Replay replay) {
        this.replay = replay;

        this.loaded.addListener((ChangeListener<Boolean>) (l, oldValue, newValue) -> {
            if (oldValue != newValue) {
                this.writeToHeader(true);
            }
        });

        this.comments.addListener((ListChangeListener<String>) c -> writeToHeader(true));
    }

    public Replay getReplay() {
        return replay;
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

    public void addComment(String comment) {
        this.comments.add(comment);
    }

    public void removeComment(int index) {
        this.comments.remove(index);
    }

    public void setComments(Collection<String> comments) {
        this.comments.setAll(comments);
    }

    public void setComments(Iterator<String> comments) {
        this.comments.clear();
        comments.forEachRemaining(this.comments::add);
    }

    public void setComments(String... comments) {
        this.comments.setAll(comments);
    }

    public void clearComments() {
        this.comments.clear();
    }

    public List<String> getComments() {
        return Collections.unmodifiableList(comments.get());
    }

    public ListProperty<String> commentsProperty() {
        return comments;
    }

    public void writeToHeader(boolean async) {
        if (async) {
            App.getInstance().getExecutor().execute(this::writeToHeader);
        } else {
            this.writeToHeader();
        }
    }

    public void writeToHeader() {
        File headerFile = replay.getHeaderJsonFile();

        JsonObject root = null;
        try (FileReader reader = new FileReader(headerFile)) {
            root = App.GSON.fromJson(reader, JsonObject.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (root == null) {
            throw new UnsupportedOperationException("Could not properly read JSON from header for file " + headerFile.getAbsolutePath());
        }

        JsonObject aftershockRoot = JsonUtil.getOrCreate(root, "aftershock", JsonElement::getAsJsonObject, JsonObject::add, new JsonObject());
        aftershockRoot.addProperty("loaded", loaded.getValue());

        JsonArray commentsArray = new JsonArray(comments.size());
        this.comments.forEach(commentsArray::add);
        aftershockRoot.add("comments", commentsArray);

        JsonArray tagsArray = new JsonArray(); // TODO
        aftershockRoot.add("tags", tagsArray);

        try (JsonWriter writer = App.GSON.newJsonWriter(new FileWriter(headerFile))) {
            App.GSON.toJson(root, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
