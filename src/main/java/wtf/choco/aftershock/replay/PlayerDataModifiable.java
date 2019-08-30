package wtf.choco.aftershock.replay;

import java.util.Objects;

public final class PlayerDataModifiable implements PlayerData {

    String name;
    Platform platform;
    Team team;
    int score, goals, assists, saves, shots;

    private final Replay replay;

    PlayerDataModifiable(Replay replay) {
        this.replay = replay;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Platform getPlatform() {
        return platform;
    }

    @Override
    public Team getTeam() {
        return team;
    }

    @Override
    public int getScore() {
        return score;
    }

    @Override
    public int getGoals() {
        return goals;
    }

    @Override
    public int getAssists() {
        return assists;
    }

    @Override
    public int getSaves() {
        return saves;
    }

    @Override
    public int getShots() {
        return shots;
    }

    @Override
    public Replay getReplay() {
        return replay;
    }

    @Override
    public int hashCode() {
        return (name != null) ? name.hashCode() : 0;
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof PlayerDataModifiable && Objects.equals(name, ((PlayerDataModifiable) obj).name));
    }

}
