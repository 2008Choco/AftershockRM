package wtf.choco.aftershock.structure;

import javafx.scene.paint.Color;

import java.util.UUID;

public class Tag {

    private final UUID uuid;
    private final String name;
    private final Color colour;

    public Tag(UUID uuid, String name, Color colour) {
        this.uuid = uuid;
        this.name = name;
        this.colour = colour;
    }

    public Tag(String name, Color colour) {
        this(UUID.randomUUID(), name, colour);
    }

    public UUID getUUID() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public Color getColour() {
        return colour;
    }

    @Override
    public String toString() {
        return name + "[" + colour.getRed() + "," + colour.getGreen() + "," + colour.getBlue() + "]";
    }

}
