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

import java.util.List;

public final class AftershockData {

    private final BooleanProperty loaded;
    private final StringProperty comments;
    private final ListProperty<Tag> tags;

    public AftershockData(boolean loaded, String comments, List<Tag> tags) {
        this.loaded = new SimpleBooleanProperty(loaded);
        this.comments = new SimpleStringProperty(comments);
        this.tags = new SimpleListProperty<>(FXCollections.observableArrayList(tags));
    }

    public AftershockData() {
        this.loaded = new SimpleBooleanProperty(true);
        this.comments = new SimpleStringProperty("");
        this.tags = new SimpleListProperty<>(FXCollections.observableArrayList());
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

    public ObservableList<Tag> getTags() {
        return tags.get();
    }

    public ListProperty<Tag> tagsProperty() {
        return tags;
    }

}
