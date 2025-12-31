package wtf.choco.aftershock.replay;

public enum Team {

    BLUE,
    ORANGE;

    public static Team fromInternalId(int id) {
        return (id == 0) ? BLUE : ORANGE;
    }

}
