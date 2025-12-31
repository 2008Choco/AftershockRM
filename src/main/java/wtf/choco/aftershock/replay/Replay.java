package wtf.choco.aftershock.replay;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

import wtf.choco.aftershock.structure.ReplayEntry;

import javafx.scene.Parent;

public interface Replay {

    public File getDemoFile();

    public File getCachedFile();

    public File getHeaderJsonFile();

    public int getTeamSize();

    public int getScore(Team team);

    public List<PlayerData> getPlayers();

    public List<GoalData> getGoals();

    public String getId();

    public String getName();

    public String getMapName();

    public String getPlayerName();

    public LocalDateTime getDate();

    public float getFPS();

    public int getLength();

    public int getVersion();

    public ReplayEntry getEntryData();

    public Parent getInfoPanel();

}
