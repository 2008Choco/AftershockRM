package wtf.choco.aftershock.util;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import wtf.choco.aftershock.App;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;

public final class FXUtils {

    private FXUtils() { }

    public static <T extends Node, C> LoadedFXMLObject<T, C> loadFXML(URL location, ResourceBundle resources) {
        try {
            FXMLLoader loader = new FXMLLoader(location, resources);
            return new LoadedFXMLObject<>(loader.load(), loader.getController());
        } catch (IOException e) {
            App.LOGGER.log(Level.SEVERE, "Failed to load FXML file: " + location, e);
            Platform.exit();
            throw new RuntimeException(e); // Just throwing an exception so we don't get any "potential NPE" warnings from our IDE
        }
    }

    public static <T> T loadFXMLRoot(URL location, ResourceBundle resources) {
        try {
            return FXMLLoader.load(location, resources);
        } catch (IOException e) {
            App.LOGGER.log(Level.SEVERE, "Failed to load FXML file: " + location, e);
            Platform.exit();
            throw new RuntimeException(e); // Just throwing an exception so we don't get any "potential NPE" warnings from our IDE
        }
    }

    public static void loadFXMLComponent(URL location, Object componentObject, ResourceBundle resources) {
        FXMLLoader loader = new FXMLLoader(location, resources);
        loader.setRoot(componentObject);
        loader.setController(componentObject);

        try {
            loader.load();
        } catch (IOException e) {
            Platform.exit();
            throw new RuntimeException(e);
        }
    }

}
