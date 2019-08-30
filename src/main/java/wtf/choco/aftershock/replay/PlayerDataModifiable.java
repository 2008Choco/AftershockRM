package wtf.choco.aftershock.replay;

import java.util.Objects;

public final class PlayerDataModifiable implements PlayerData {

    private String name;
    private Platform platform;
    private Team team;
    private int score, goals, assists, saves, shots;

    private final Replay replay;

    PlayerDataModifiable(Replay replay) {
        this.replay = replay;
    }

    PlayerDataModifiable name(String name) {
        this.name = name;
        return this;
    }

    @Override
    public String getName() {
        return name;
    }

    PlayerDataModifiable platform(Platform platform) {
        this.platform = platform;
        return this;
    }

    @Override
    public Platform getPlatform() {
        return platform;
    }

    PlayerDataModifiable team(Team team) {
        this.team = team;
        return this;
    }

    @Override
    public Team getTeam() {
        return team;
    }

    PlayerDataModifiable score(int score) {
        this.score = score;
        return this;
    }

    @Override
    public int getScore() {
        return score;
    }

    PlayerDataModifiable goals(int goals) {
        this.goals = goals;
        return this;
    }

    @Override
    public int getGoals() {
        return goals;
    }

    PlayerDataModifiable assists(int assists) {
        this.assists = assists;
        return this;
    }

    @Override
    public int getAssists() {
        return assists;
    }

    PlayerDataModifiable saves(int saves) {
        this.saves = saves;
        return this;
    }

    @Override
    public int getSaves() {
        return saves;
    }

    PlayerDataModifiable shots(int shots) {
        this.shots = shots;
        return this;
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
