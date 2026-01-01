package wtf.choco.aftershock.replay;

import wtf.choco.aftershock.structure.Tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AftershockData {

    private boolean loaded;
    private String comments;
    private final List<Tag> tags;

    public AftershockData() {
        this.loaded = true;
        this.comments = "";
        this.tags = new ArrayList<>();
    }

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getComments() {
        return comments;
    }

    public void addTag(Tag tag) {
        this.tags.add(tag);
    }

    public List<Tag> getTags() {
        return Collections.unmodifiableList(tags);
    }

}
