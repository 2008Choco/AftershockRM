package wtf.choco.aftershock.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import wtf.choco.aftershock.App;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public final class FXUtils {

    private FXUtils() { }

    public static <T extends Node, C> LoadedFXMLObject<T, C> loadFXML(String path, ResourceBundle resources) {
        URL fxmlLocation = getURL(path);

        try {
            FXMLLoader loader = new FXMLLoader(fxmlLocation, resources);
            return new LoadedFXMLObject<>(loader.load(), loader.getController());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
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

    public static void loadFXMLComponent(String path, Object componentObject, ResourceBundle resources) {
        FXMLLoader loader = new FXMLLoader(getURL(path), resources);
        loader.setRoot(componentObject);
        loader.setController(componentObject);

        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
