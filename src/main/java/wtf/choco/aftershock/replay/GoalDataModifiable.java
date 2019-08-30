package wtf.choco.aftershock.replay;

import java.util.Objects;

public final class GoalDataModifiable implements GoalData {

    private int secondsIn;
    private PlayerData player;
    private Team team;

    private final Replay replay;

    GoalDataModifiable(Replay replay) {
        this.replay = replay;
    }

    GoalDataModifiable secondsIn(int seconds) {
        this.secondsIn = seconds;
        return this;
    }

    @Override
    public int getSecondsIn() {
        return secondsIn;
    }

    GoalDataModifiable player(PlayerData player) {
        this.player = player;
        return this;
    }

    @Override
    public PlayerData getPlayer() {
        return player;
    }

    GoalDataModifiable team(Team team) {
        this.team = team;
        return this;
    }

    @Override
    public Team getTeam() {
        return team;
    }

    @Override
    public Replay getReplay() {
        return replay;
    }

    @Override
    public int hashCode() {
        return Objects.hash(secondsIn, player, team);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof GoalDataModifiable)) return false;

        GoalDataModifiable other = (GoalDataModifiable) obj;
        return secondsIn == other.secondsIn && Objects.equals(player, other.player) && team == other.team;
    }

}
