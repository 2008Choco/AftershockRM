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
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;

import wtf.choco.aftershock.App;
import wtf.choco.aftershock.controller.InfoPanelController;
import wtf.choco.aftershock.manager.TagRegistry;
import wtf.choco.aftershock.structure.ReplayEntry;
import wtf.choco.aftershock.structure.Tag;
import wtf.choco.aftershock.util.JsonUtil;
import wtf.choco.aftershock.util.TriConsumer;

import javafx.scene.Parent;

public final class ReplayModifiable implements Replay {

    private final File demoFile, cachedFile, headerFile;

    private int teamSize;
    private int blueScore, orangeScore;
    private List<PlayerData> playerData = Collections.EMPTY_LIST;
    private List<GoalData> goalData = Collections.EMPTY_LIST;
    private String name, id, mapName, playerName;
    private LocalDateTime date;
    private int replayVersion;
    private int length;
    private float fps;

    private ReplayEntry entryData;
    private Parent infoPanel;

    private boolean modifiedHeader = false;

    public ReplayModifiable(Gson gson, File demoFile, File cachedFile, File headerFile) {
        this.demoFile = demoFile;
        this.cachedFile = cachedFile;
        this.headerFile = headerFile;

        if (gson != null) {
            this.loadDataFromFile();
        }
    }

    public ReplayModifiable(File demoFile, File cachedFile, File headerFile) {
        this(null, demoFile, cachedFile, headerFile);
    }

    @Override
    public File getDemoFile() {
        return demoFile;
    }

    @Override
    public File getCachedFile() {
        return cachedFile;
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
    public float getFPS() {
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
    public Parent getInfoPanel() {
        if (infoPanel == null) {
            this.infoPanel = InfoPanelController.createInfoPanelFor(this, App.getInstance().getResources());
        }

        return infoPanel;
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
        App app = App.getInstance();

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
        if (!root.has("aftershock")) {
            this.modifiedHeader = true;
        }

        JsonObject aftershockRoot = getOrCreate(root, "aftershock", JsonElement::getAsJsonObject, JsonObject::add, JsonObject::new);
        this.entryData = new ReplayEntry(this);
        this.entryData.setLoaded(JsonUtil.getOrCreate(aftershockRoot, "loaded", JsonElement::getAsBoolean, JsonObject::addProperty, true));
        this.entryData.setComments(JsonUtil.getOrCreate(aftershockRoot, "comments", JsonElement::getAsString, JsonObject::addProperty, ""));

        JsonArray tags = getOrCreate(aftershockRoot, "tags", JsonElement::getAsJsonArray, JsonObject::add, (Supplier<JsonArray>) JsonArray::new);
        if (tags.size() > 0) {
            TagRegistry tagRegistry = app.getTagRegistry();

            for (JsonElement tagIdElement : tags) {
                UUID tagUUID = UUID.fromString(tagIdElement.getAsString());
                Tag tag = tagRegistry.getTag(tagUUID);
                if (tag == null) {
                    app.getLogger().warning("Attempted to load tag with unknown UUID " + (tagUUID) + ". Ignoring...");
                    this.modifiedHeader = true; // Mark as dirty to remove missing tag
                    continue;
                }

                this.entryData.addTag(tag);
            }
        }

        this.entryData.registerPropertyListeners(app);

        JsonObject propertiesObject = root.getAsJsonObject("Properties");

        // Basic primitive data
        this.teamSize = JsonUtil.getInt(propertiesObject, "TeamSize", 3);
        if (playerData.isEmpty()) {
            this.playerData = new ArrayList<>(teamSize * 2);
        }

        this.blueScore = JsonUtil.getInt(propertiesObject, "Team0Score", 0);
        this.orangeScore = JsonUtil.getInt(propertiesObject, "Team1Score", 0);
        this.replayVersion = JsonUtil.getInt(propertiesObject, "ReplayVersion", -1);

        String mapId = JsonUtil.getString(JsonUtil.getObject(propertiesObject, "MapName"), "Value", "UNKNOWN_MAP");
        String mapTranslationKey = "map.name." + mapId.toLowerCase();
        this.mapName = (mapId != null && app.getResources().containsKey(mapTranslationKey) ? app.getResources().getString(mapTranslationKey) : "%unknown_map__" + mapId + "__%");
        this.name = JsonUtil.getString(propertiesObject, "ReplayName", "[" + getMapName() + " - " + teamSize + "v" + teamSize + "]");
        this.id = JsonUtil.getString(propertiesObject, "Id", cachedFile.getName().substring(0, cachedFile.getName().lastIndexOf('.')));
        this.playerName = JsonUtil.getString(propertiesObject, "PlayerName", "%unknown_player%");
        this.fps = JsonUtil.getFloat(propertiesObject, "RecordFPS", 30.0F);
        this.length = (int) (JsonUtil.getInt(propertiesObject, "NumFrames", -1) / fps);

        // More complex data
        String dateString = JsonUtil.getString(propertiesObject, "Date", "1970-00-00 00-00-00");
        this.date = LocalDateTime.parse(dateString, DateTimeFormatter.ofPattern("uuuu-MM-dd HH-mm-ss"));

        /* Players */
        Map<String, PlayerData> nameToPlayerData = new HashMap<>();
        JsonArray players = JsonUtil.getArray(propertiesObject, "PlayerStats");
        for (JsonElement playerElement : players) {
            if (!playerElement.isJsonObject()) {
                throw new IllegalStateException("Expected player object, received " + playerElement.getClass().getSimpleName());
            }

            JsonObject playerRoot = playerElement.getAsJsonObject();
            PlayerDataModifiable playerData = new PlayerDataModifiable(this);

            playerData.name = JsonUtil.getString(playerRoot, "Name", "%unknown_player%");
            playerData.team = Team.fromInternalId(JsonUtil.getInt(playerRoot, "Team", 0));
            playerData.score = JsonUtil.getInt(playerRoot, "Score", 0);
            playerData.goals = JsonUtil.getInt(playerRoot, "Goals", 0);
            playerData.assists = JsonUtil.getInt(playerRoot, "Assists", 0);
            playerData.saves = JsonUtil.getInt(playerRoot, "Saves", 0);
            playerData.shots = JsonUtil.getInt(playerRoot, "Shots", 0);

            nameToPlayerData.put(playerData.getName(), playerData);
            this.addPlayer(playerData);
        }

        /* Goals */
        JsonArray goals = JsonUtil.getArray(propertiesObject, "Goals");
        for (JsonElement goalElement : goals) {
            if (!goalElement.isJsonObject()) {
                throw new IllegalStateException("Expected goal object, received " + goalElement.getClass().getSimpleName());
            }

            JsonObject goalRoot = goalElement.getAsJsonObject();
            GoalDataModifiable goalData = new GoalDataModifiable(this);

            goalData.secondsIn = (int) (JsonUtil.getInt(goalRoot, "frame", 0) / fps);
            goalData.team = Team.fromInternalId(JsonUtil.getInt(goalRoot, "PlayerTeam", 0));
            goalData.player = nameToPlayerData.get(JsonUtil.getString(goalRoot, "PlayerName", ""));

            this.addGoal(goalData);
        }

        if (modifiedHeader) {
            app.getLogger().info("(" + App.truncateID(id) + ") " + "- Writing aftershock data to header");
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
