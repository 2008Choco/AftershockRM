package wtf.choco.aftershock.replay;

public interface PlayerData {

    public String getName();

    public Team getTeam();

    public int getScore();

    public int getGoals();

    public int getAssists();

    public int getSaves();

    public int getShots();

    public Replay getReplay();

}
