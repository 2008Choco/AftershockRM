package wtf.choco.aftershock.replay;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import wtf.choco.aftershock.App;

public final class ReplayModifiable implements Replay {

    private final File demoFile, headerFile;

    private int teamSize;
    private int blueScore, orangeScore;
    private List<PlayerData> playerData = Collections.EMPTY_LIST;
    private List<GoalData> goalData = Collections.EMPTY_LIST;
    private String name, id, mapName, playerName;
    private LocalDateTime date;
    private int replayVersion;
    private int length, fps;

    public ReplayModifiable(Gson gson, File demoFile, File headerFile) {
        this.demoFile = demoFile;
        this.headerFile = headerFile;

        if (gson != null) {
            this.loadDataFromFile(gson);
        }
    }

    public ReplayModifiable(File demoFile, File headerFile) {
        this(null, demoFile, headerFile);
    }

    @Override
    public File getDemoFile() {
        return demoFile;
    }

    @Override
    public File getHeaderJsonFile() {
        return headerFile;
    }

    ReplayModifiable teamSize(int teamSize) {
        this.teamSize = teamSize;

        if (playerData == Collections.EMPTY_LIST) {
            this.playerData = new ArrayList<>(teamSize * 2);
        }

        return this;
    }

    @Override
    public int getTeamSize() {
        return teamSize;
    }

    ReplayModifiable blueScore(int score) {
        this.blueScore = score;
        return this;
    }

    ReplayModifiable orangeScore(int score) {
        this.orangeScore = score;
        return this;
    }

    @Override
    public int getScore(Team team) {
        return (team == Team.BLUE) ? blueScore : orangeScore;
    }

    ReplayModifiable addPlayer(PlayerData player) {
        if (playerData == Collections.EMPTY_LIST) {
            this.playerData = new ArrayList<>((teamSize == 0 ? 6 : (teamSize * 2)));
        }

        this.playerData.add(player);
        return this;
    }

    @Override
    public List<PlayerData> getPlayers() {
        return Collections.unmodifiableList(playerData);
    }

    ReplayModifiable addGoal(GoalData goal) {
        if (goalData == Collections.EMPTY_LIST) {
            this.goalData = new ArrayList<>();
        }

        this.goalData.add(goal);
        return this;
    }

    @Override
    public List<GoalData> getGoals() {
        return Collections.unmodifiableList(goalData);
    }

    ReplayModifiable name(String name) {
        this.name = name;
        return this;
    }

    @Override
    public String getName() {
        return name;
    }

    ReplayModifiable id(String id) {
        this.id = id;
        return this;
    }

    @Override
    public String getId() {
        return id;
    }

    ReplayModifiable mapName(String name) {
        this.mapName = name;
        return this;
    }

    @Override
    public String getMapName() {
        return mapName;
    }

    ReplayModifiable playerName(String name) {
        this.playerName = name;
        return this;
    }

    @Override
    public String getPlayerName() {
        return playerName;
    }

    ReplayModifiable date(LocalDateTime date) {
        this.date = date;
        return this;
    }

    @Override
    public LocalDateTime getDate() {
        return date;
    }

    ReplayModifiable length(int length) {
        this.length = length;
        return this;
    }

    @Override
    public int getLength() {
        return length;
    }

    ReplayModifiable fps(int fps) {
        this.fps = fps;
        return this;
    }

    @Override
    public int getFPS() {
        return fps;
    }

    ReplayModifiable version(int version) {
        this.replayVersion = version;
        return this;
    }

    @Override
    public int getVersion() {
        return replayVersion;
    }

    @Override
    public int hashCode() {
        return (id != null) ? id.hashCode() : 0;
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof ReplayModifiable && Objects.equals(id, ((ReplayModifiable) obj).id));
    }

    public void loadDataFromFile(Gson gson) {
        JsonObject root = null;
        try (FileReader reader = new FileReader(headerFile)) {
            root = gson.fromJson(reader, JsonObject.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (root == null) {
            throw new UnsupportedOperationException("Could not properly read JSON from header for file " + headerFile.getAbsolutePath());
        }

        JsonObject header = root.getAsJsonObject("header").getAsJsonObject("body").getAsJsonObject("properties").getAsJsonObject("value");

        // Basic primitive data
        this.teamSize(get(header, "TeamSize", "int", JsonElement::getAsInt));
        this.blueScore(get(header, "Team0Score", "int", JsonElement::getAsInt, 0));
        this.orangeScore(get(header, "Team1Score", "int", JsonElement::getAsInt, 0));
        this.version(get(header, "ReplayVersion", "int", JsonElement::getAsInt));

        String mapId = get(header, "MapName", "name", JsonElement::getAsString);
        this.mapName(mapId != null ? App.getInstance().getResources().getString("map.name." + mapId.toLowerCase()) : "%unknown_map%");
        this.name(get(header, "ReplayName", "str", JsonElement::getAsString, "[" + getMapName() + " - " + teamSize + "v" + teamSize + "]"));
        this.id(get(header, "Id", "str", JsonElement::getAsString));
        this.playerName(get(header, "PlayerName", "str", JsonElement::getAsString));
        this.fps(get(header, "RecordFPS", "float", JsonElement::getAsInt, 30));
        this.length(get(header, "NumFrames", "int", JsonElement::getAsInt, -this.fps) / this.fps); // (defaults to -1)

        // More complex data
        String dateString = get(header, "Date", "str", JsonElement::getAsString, "1970-00-00 00-00-00");
        LocalDateTime date = LocalDateTime.parse(dateString, DateTimeFormatter.ofPattern("uuuu-MM-dd HH-mm-ss"));
        this.date((date != null) ? date : LocalDateTime.MIN);

        /* Players */
        Map<String, PlayerData> nameToPlayerData = new HashMap<>();
        JsonArray players = get(header, "PlayerStats", "array", JsonElement::getAsJsonArray, new JsonArray(0));
        for (JsonElement playerElement : players) {
            if (!playerElement.isJsonObject()) {
                throw new IllegalStateException("Expected player object, received " + playerElement.getClass().getSimpleName());
            }

            JsonObject playerRoot = playerElement.getAsJsonObject().getAsJsonObject("value");
            PlayerDataModifiable playerData = new PlayerDataModifiable(this);

            playerData.name(get(playerRoot, "Name", "str", JsonElement::getAsString));
            playerData.team(Team.fromInternalId(get(playerRoot, "Team", "int", JsonElement::getAsInt)));
            // TODO: Parse platform
            playerData.score(get(playerRoot, "Score", "int", JsonElement::getAsInt, 0));
            playerData.goals(get(playerRoot, "Goals", "int", JsonElement::getAsInt, 0));
            playerData.assists(get(playerRoot, "Assists", "int", JsonElement::getAsInt, 0));
            playerData.saves(get(playerRoot, "Saves", "int", JsonElement::getAsInt, 0));
            playerData.shots(get(playerRoot, "Shots", "int", JsonElement::getAsInt, 0));

            nameToPlayerData.put(playerData.getName(), playerData);
            this.addPlayer(playerData);
        }

        /* Goals */
        JsonArray goals = get(header, "Goals", "array", JsonElement::getAsJsonArray, new JsonArray(0));
        for (JsonElement goalElement : goals) {
            if (!goalElement.isJsonObject()) {
                throw new IllegalStateException("Expected goal object, received " + goalElement.getClass().getSimpleName());
            }

            JsonObject goalRoot = goalElement.getAsJsonObject().getAsJsonObject("value");
            GoalDataModifiable goalData = new GoalDataModifiable(this);

            goalData.secondsIn(get(goalRoot, "frame", "int", JsonElement::getAsInt) / fps);
            goalData.team(Team.fromInternalId(get(goalRoot, "PlayerTeam", "int", JsonElement::getAsInt)));
            goalData.player(nameToPlayerData.get(get(goalRoot, "PlayerName", "str", JsonElement::getAsString)));

            this.addGoal(goalData);
        }
    }

    private <T> T get(JsonObject root, String propertyKey, String type, Function<JsonElement, T> caster) {
        return get(root, propertyKey, type, caster, null);
    }

    private <T> T get(JsonObject root, String propertyKey, String type, Function<JsonElement, T> caster, T defaultValue) {
        if (!root.has(propertyKey)) {
            return defaultValue;
        }

        JsonObject element = root.getAsJsonObject(propertyKey);
        return caster.apply(element.getAsJsonObject("value").get(type));
    }

}
