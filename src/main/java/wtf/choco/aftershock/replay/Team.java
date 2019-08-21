package wtf.choco.aftershock.replay;

public enum Team {

    BLUE,
    ORANGE;


    public int internalId() { // A wrapper method to make it a bit more readable
        return ordinal();
    }

    public static Team fromInternalId(int id) {
        return (id == 0) ? BLUE : ORANGE;
    }

}
