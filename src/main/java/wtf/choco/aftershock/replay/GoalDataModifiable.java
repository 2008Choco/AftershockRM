package wtf.choco.aftershock.replay;

import java.util.Objects;

public final class GoalDataModifiable implements GoalData {

    int secondsIn;
    PlayerData player;
    Team team;

    private final Replay replay;

    GoalDataModifiable(Replay replay) {
        this.replay = replay;
    }

    @Override
    public int getSecondsIn() {
        return secondsIn;
    }

    @Override
    public PlayerData getPlayer() {
        return player;
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
