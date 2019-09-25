package wtf.choco.aftershock.util;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.function.Function;

import wtf.choco.aftershock.App;

import javafx.fxml.FXMLLoader;
import javafx.util.Pair;

public final class FXUtils {

    private FXUtils() { }

    public static <T, C> Pair<T, C> loadFXML(String path, ResourceBundle resources) {
        URL fxmlLocation = getURL(path);

        try {
            FXMLLoader loader = new FXMLLoader(fxmlLocation, resources);
            return new Pair<>(loader.load(), loader.getController());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static <T, C> Pair<T, C> loadFXML(String path) {
        return loadFXML(path, null);
    }

    public static <T> T loadFXMLRoot(String path, ResourceBundle resources) {
        URL fxmlLocation = getURL(path);

        try {
            return FXMLLoader.load(fxmlLocation, resources);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static <T> T loadFXMLRoot(String path) {
        return loadFXMLRoot(path, null);
    }

    public static <T> PublicTask<T> createTask(Function<PublicTask<T>, T> task) {
        return new PublicTask<>() {
            @Override
            protected T call() throws Exception {
                return task.apply(this);
            }
        };
    }

    public static <T> PublicTask<T> createTask(Consumer<PublicTask<T>> task) {
        return new PublicTask<>() {
            @Override
            protected T call() throws Exception {
                task.accept(this);
                return null;
            }
        };
    }

    private static URL getURL(String path) {
        Preconditions.checkNotEmpty(path, "Cannot load null or empty path");

        if (!path.endsWith(".fxml")) {
            path = path.concat(".fxml");
        }

        URL fxmlLocation = App.class.getResource(path);
        if (fxmlLocation == null) {
            throw new IllegalArgumentException("Could not find FXML file at path \"" + path + "\"");
        }

        return fxmlLocation;
    }

}
