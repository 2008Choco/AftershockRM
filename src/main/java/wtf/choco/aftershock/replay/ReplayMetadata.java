package wtf.choco.aftershock.replay;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import wtf.choco.aftershock.structure.Tag;

import java.util.Collections;
import java.util.List;

public final class ReplayMetadata {

    private final BooleanProperty loaded;
    private final StringProperty comment;
    private final ListProperty<Tag> tags;

    public ReplayMetadata(boolean loaded, String comment, List<Tag> tags) {
        this.loaded = new SimpleBooleanProperty(this, "loaded", loaded);
        this.comment = new SimpleStringProperty(this, "comment", comment);
        this.tags = new SimpleListProperty<>(this, "tags", FXCollections.observableArrayList(tags));
    }

    public ReplayMetadata() {
        this(true, "", Collections.emptyList());
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

    public void setComment(String comment) {
        this.comment.set(comment);
    }

    public String getComment() {
        return comment.get();
    }

    public StringProperty commentProperty() {
        return comment;
    }

    public void addTag(Tag tag) {
        this.tags.add(tag);
    }

    public ObservableList<Tag> getTags() {
        return tags.get();
    }

    public ListProperty<Tag> tagsProperty() {
        return tags;
    }

}
