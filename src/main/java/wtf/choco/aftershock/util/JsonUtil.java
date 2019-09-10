package wtf.choco.aftershock.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;

import wtf.choco.aftershock.App;

public final class JsonUtil {

    private JsonUtil() { }

    public static <T> T get(JsonObject root, String propertyKey, String type, Function<JsonElement, T> caster) {
        return get(root, propertyKey, type, caster, null);
    }

    public static <T> T get(JsonObject root, String propertyKey, String type, Function<JsonElement, T> caster, T defaultValue) {
        if (!root.has(propertyKey)) {
            return defaultValue;
        }

        JsonObject element = root.getAsJsonObject(propertyKey);
        return caster.apply(element.getAsJsonObject("value").get(type));
    }

    public static <T> T getOrCreate(JsonObject root, String key, Function<JsonElement, T> retriever, TriConsumer<JsonObject, String, T> addFunction, T defaultValue) {
        if (!root.has(key)) {
            addFunction.accept(root, key, defaultValue);
        }

        return retriever.apply(root.get(key));
    }

    public static <T> T getOrCreate(JsonObject root, String key, Function<JsonElement, T> retriever, TriConsumer<JsonObject, String, T> addFunction, Supplier<T> defaultValue) {
        if (!root.has(key)) {
            addFunction.accept(root, key, defaultValue.get());
        }

        return retriever.apply(root.get(key));
    }

    public static <T> T loadFromFile(File file, Class<T> type) {
        T root = null;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))){
            root = App.GSON.fromJson(reader, type);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return root;
    }

    public static void writeToFile(File file, JsonElement root) {
        try (JsonWriter writer = App.GSON.newJsonWriter(new FileWriter(file))){
            App.GSON.toJson(root, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
