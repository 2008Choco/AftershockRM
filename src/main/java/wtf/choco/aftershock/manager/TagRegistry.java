package wtf.choco.aftershock.manager;

import javafx.scene.paint.Color;
import wtf.choco.aftershock.structure.Tag;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TagRegistry {

    private final Map<UUID, Tag> tags = new HashMap<>();

    public void registerTag(Tag tag) {
        this.tags.put(tag.getUUID(), tag);
    }

    public Tag createTag(String name, Color colour) {
        Tag tag = new Tag(name, colour);
        this.registerTag(tag);
        return tag;
    }

    public void unregisterTag(Tag tag) {
        this.tags.remove(tag.getUUID());
    }

    public Tag getTag(UUID uuid) {
        return tags.get(uuid);
    }

    public Collection<Tag> getTags() {
        return Collections.unmodifiableCollection(tags.values());
    }

    public void clearTags() {
        this.tags.clear();
    }

}
