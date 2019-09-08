package wtf.choco.aftershock.replay;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
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
import java.util.function.Supplier;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;

import wtf.choco.aftershock.App;
import wtf.choco.aftershock.structure.ReplayEntry;
import wtf.choco.aftershock.util.JsonUtil;
import wtf.choco.aftershock.util.TriConsumer;

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

    private ReplayEntry entryData;

    private boolean modifiedHeader = false;

    public ReplayModifiable(Gson gson, File demoFile, File headerFile) {
        this.demoFile = demoFile;
        this.headerFile = headerFile;

        if (gson != null) {
            this.loadDataFromFile();
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

    @Override
    public int getTeamSize() {
        return teamSize;
    }

    @Override
    public int getScore(Team team) {
        return (team == Team.BLUE) ? blueScore : orangeScore;
    }

    private void addPlayer(PlayerData player) {
        if (playerData == Collections.EMPTY_LIST) {
            this.playerData = new ArrayList<>((teamSize == 0 ? 6 : (teamSize * 2)));
        }

        this.playerData.add(player);
    }

    @Override
    public List<PlayerData> getPlayers() {
        return Collections.unmodifiableList(playerData);
    }

    private void addGoal(GoalData goal) {
        if (goalData == Collections.EMPTY_LIST) {
            this.goalData = new ArrayList<>();
        }

        this.goalData.add(goal);
    }

    @Override
    public List<GoalData> getGoals() {
        return Collections.unmodifiableList(goalData);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getMapName() {
        return mapName;
    }

    @Override
    public String getPlayerName() {
        return playerName;
    }

    @Override
    public LocalDateTime getDate() {
        return date;
    }

    @Override
    public int getLength() {
        return length;
    }

    @Override
    public int getFPS() {
        return fps;
    }

    @Override
    public int getVersion() {
        return replayVersion;
    }

    @Override
    public ReplayEntry getEntryData() {
        return entryData;
    }

    @Override
    public int hashCode() {
        return (id != null) ? id.hashCode() : 0;
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || (obj instanceof ReplayModifiable && Objects.equals(id, ((ReplayModifiable) obj).id));
    }

    public void loadDataFromFile() {
        JsonObject root = null;
        try (FileReader reader = new FileReader(headerFile)) {
            root = App.GSON.fromJson(reader, JsonObject.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (root == null) {
            throw new UnsupportedOperationException("Could not properly read JSON from header for file " + headerFile.getAbsolutePath());
        }

        // Aftershock-specific data
        JsonObject aftershockRoot = getOrCreate(root, "aftershock", JsonElement::getAsJsonObject, JsonObject::add, JsonObject::new);
        this.entryData = new ReplayEntry(this);
        this.entryData.setLoaded(JsonUtil.getOrCreate(aftershockRoot, "loaded", JsonElement::getAsBoolean, JsonObject::addProperty, true));

        JsonArray comments = getOrCreate(aftershockRoot, "comments", JsonElement::getAsJsonArray, JsonObject::add, (Supplier<JsonArray>) JsonArray::new);
        if (comments.size() > 0) {
            comments.forEach(e -> entryData.addComment(e.getAsString()));
        }

        JsonArray tags = getOrCreate(aftershockRoot, "tags", JsonElement::getAsJsonArray, JsonObject::add, (Supplier<JsonArray>) JsonArray::new);
        if (tags.size() > 0) {
            // TODO
        }

        JsonObject header = root.getAsJsonObject("header").getAsJsonObject("body").getAsJsonObject("properties").getAsJsonObject("value");

        // Basic primitive data
        this.teamSize = JsonUtil.get(header, "TeamSize", "int", JsonElement::getAsInt);
        if (playerData == Collections.EMPTY_LIST) {
            this.playerData = new ArrayList<>(teamSize * 2);
        }

        this.blueScore = JsonUtil.get(header, "Team0Score", "int", JsonElement::getAsInt, 0);
        this.orangeScore = JsonUtil.get(header, "Team1Score", "int", JsonElement::getAsInt, 0);
        this.replayVersion = JsonUtil.get(header, "ReplayVersion", "int", JsonElement::getAsInt);

        String mapId = JsonUtil.get(header, "MapName", "name", JsonElement::getAsString);
        this.mapName = (mapId != null ? App.getInstance().getResources().getString("map.name." + mapId.toLowerCase()) : "%unknown_map%");
        this.name = JsonUtil.get(header, "ReplayName", "str", JsonElement::getAsString, "[" + getMapName() + " - " + teamSize + "v" + teamSize + "]");
        this.id = JsonUtil.get(header, "Id", "str", JsonElement::getAsString);
        this.playerName = JsonUtil.get(header, "PlayerName", "str", JsonElement::getAsString);
        this.fps = JsonUtil.get(header, "RecordFPS", "float", JsonElement::getAsInt, 30);
        this.length = JsonUtil.get(header, "NumFrames", "int", JsonElement::getAsInt, -fps) / fps; // (defaults to -1)

        // More complex data
        String dateString = JsonUtil.get(header, "Date", "str", JsonElement::getAsString, "1970-00-00 00-00-00");
        LocalDateTime date = LocalDateTime.parse(dateString, DateTimeFormatter.ofPattern("uuuu-MM-dd HH-mm-ss"));
        this.date = ((date != null) ? date : LocalDateTime.MIN);

        /* Players */
        Map<String, PlayerData> nameToPlayerData = new HashMap<>();
        JsonArray players = JsonUtil.get(header, "PlayerStats", "array", JsonElement::getAsJsonArray, new JsonArray(0));
        for (JsonElement playerElement : players) {
            if (!playerElement.isJsonObject()) {
                throw new IllegalStateException("Expected player object, received " + playerElement.getClass().getSimpleName());
            }

            JsonObject playerRoot = playerElement.getAsJsonObject().getAsJsonObject("value");
            PlayerDataModifiable playerData = new PlayerDataModifiable(this);

            playerData.name = JsonUtil.get(playerRoot, "Name", "str", JsonElement::getAsString);
            playerData.team = Team.fromInternalId(JsonUtil.get(playerRoot, "Team", "int", JsonElement::getAsInt));
            // TODO: Parse platform
            playerData.score = JsonUtil.get(playerRoot, "Score", "int", JsonElement::getAsInt, 0);
            playerData.goals = JsonUtil.get(playerRoot, "Goals", "int", JsonElement::getAsInt, 0);
            playerData.assists = JsonUtil.get(playerRoot, "Assists", "int", JsonElement::getAsInt, 0);
            playerData.saves = JsonUtil.get(playerRoot, "Saves", "int", JsonElement::getAsInt, 0);
            playerData.shots = JsonUtil.get(playerRoot, "Shots", "int", JsonElement::getAsInt, 0);

            nameToPlayerData.put(playerData.getName(), playerData);
            this.addPlayer(playerData);
        }

        /* Goals */
        JsonArray goals = JsonUtil.get(header, "Goals", "array", JsonElement::getAsJsonArray, new JsonArray(0));
        for (JsonElement goalElement : goals) {
            if (!goalElement.isJsonObject()) {
                throw new IllegalStateException("Expected goal object, received " + goalElement.getClass().getSimpleName());
            }

            JsonObject goalRoot = goalElement.getAsJsonObject().getAsJsonObject("value");
            GoalDataModifiable goalData = new GoalDataModifiable(this);

            goalData.secondsIn = JsonUtil.get(goalRoot, "frame", "int", JsonElement::getAsInt) / fps;
            goalData.team = Team.fromInternalId(JsonUtil.get(goalRoot, "PlayerTeam", "int", JsonElement::getAsInt));
            goalData.player = nameToPlayerData.get(JsonUtil.get(goalRoot, "PlayerName", "str", JsonElement::getAsString));

            this.addGoal(goalData);
        }

        if (modifiedHeader) {
            App.getInstance().getLogger().info("(" + App.truncateID(id) + ") " + "- Writing aftershock data to header");
            try (JsonWriter writer = App.GSON.newJsonWriter(new FileWriter(headerFile))) {
                App.GSON.toJson(root, writer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private <T> T getOrCreate(JsonObject root, String key, Function<JsonElement, T> retriever, TriConsumer<JsonObject, String, T> addFunction, Supplier<T> defaultValue) {
        if (!root.has(key)) {
            addFunction.accept(root, key, defaultValue.get());
            this.modifiedHeader = true;
        }

        return retriever.apply(root.get(key));
    }

}
