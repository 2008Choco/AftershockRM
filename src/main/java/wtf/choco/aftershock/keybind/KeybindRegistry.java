package wtf.choco.aftershock.keybind;

import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import wtf.choco.aftershock.App;
import wtf.choco.aftershock.controller.AppController;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class KeybindRegistry {

    private boolean registeredDefaults = false;

    private final Map<Node, KeybindEventHandler> keyEventHandlers = new HashMap<>();
    private final KeybindEventHandler globalHandler;
    private final App app;

    private final Set<KeyCode> down = EnumSet.noneOf(KeyCode.class);

    public KeybindRegistry(App app) {
        this.app = app;
        this.globalHandler = new KeybindEventHandler(app, null);

        Stage stage = app.getStage();
        stage.addEventFilter(KeyEvent.KEY_PRESSED, globalHandler);
        stage.addEventHandler(KeyEvent.KEY_PRESSED, event -> down.add(event.getCode()));
        stage.addEventHandler(KeyEvent.KEY_RELEASED, event -> down.remove(event.getCode()));
    }

    public KeybindData globalKeybind(KeyCode character, KeyCombination.Modifier... modifiers) {
        return registerToHandler(globalHandler, character, modifiers);
    }

    private KeybindData registerToHandler(KeybindEventHandler handler, KeyCode character, KeyCombination.Modifier... modifiers) {
        KeybindData keybind = new KeybindData(new KeyCodeCombination(character, modifiers));
        handler.addKeybind(keybind);
        return keybind;
    }

    public void clearKeybinds() {
        this.keyEventHandlers.forEach((node, handler) -> {
            handler.clearKeybinds();
            node.removeEventHandler(KeyEvent.KEY_PRESSED, handler);
        });

        this.keyEventHandlers.clear();

        this.globalHandler.clearKeybinds();
        this.app.getStage().getScene().removeEventFilter(KeyEvent.KEY_PRESSED, globalHandler);
    }

    public boolean isDown(KeyCode key) {
        return down.contains(key);
    }

    public static void registerDefaultKeybinds(KeybindRegistry registry) {
        if (registry.registeredDefaults) {
            throw new IllegalStateException("Cannot register default keybinds twice");
        }

        AppController controller = registry.app.getController();

        registry.globalKeybind(KeyCode.F, KeyCombination.CONTROL_DOWN).executes(controller.getFilterBar()::requestFocus);
        registry.globalKeybind(KeyCode.ESCAPE).executes(() -> {
            var table = controller.getReplayTable();
            if (table.getEditingCell() != null) {
                return;
            }

            controller.closeInfoPanel();
            table.getSelectionModel().clearSelection();
        });

        registry.registeredDefaults = true;
    }

}
