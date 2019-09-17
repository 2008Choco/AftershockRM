package wtf.choco.aftershock.keybind;

import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import wtf.choco.aftershock.App;
import wtf.choco.aftershock.controller.AppController;
import wtf.choco.aftershock.structure.ReplayBin;
import wtf.choco.aftershock.structure.ReplayEntry;

import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

public class KeybindRegistry {

    private boolean registeredDefaults = false;

    private final Map<Node, KeybindEventHandler> keyEventHandlers = new HashMap<>();
    private final KeybindEventHandler globalHandler;
    private final App app;

    public KeybindRegistry(App app) {
        this.app = app;
        this.globalHandler = new KeybindEventHandler(app, null);
        this.app.getStage().addEventFilter(KeyEvent.KEY_PRESSED, globalHandler);
    }

    public KeybindData globalKeybind(KeyCode character, KeyCombination.Modifier... modifiers) {
        return registerToHandler(globalHandler, character, modifiers);
    }

    public KeybindData nodedKeybind(Node node, KeyCode character, KeyCombination.Modifier... modifiers) {
        return registerToHandler(keyEventHandlers.computeIfAbsent(node, n -> new KeybindEventHandler(app, n)), character, modifiers);
    }

    private KeybindData registerToHandler(KeybindEventHandler handler, KeyCode character, KeyCombination.Modifier... modifiers) {
        KeybindData keybind = new KeybindData(new KeyCodeCombination(character, modifiers));
        handler.addKeybind(keybind);
        return keybind;
    }

    public boolean removeKeybind(KeyCode character, KeyCombination.Modifier... modifiers) {
        return globalHandler.removeKeybind(character, modifiers);
    }

    public boolean removeKeybind(Node node, KeyCode character, KeyCombination.Modifier... modifiers) {
        KeybindEventHandler handler = keyEventHandlers.get(node);
        return handler != null && handler.removeKeybind(character, modifiers);
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

    public static void registerDefaultKeybinds(KeybindRegistry registry) {
        if (registry.registeredDefaults) {
            throw new IllegalStateException("Cannot register default keybinds twice");
        }

        AppController controller = registry.app.getController();

        registry.globalKeybind(KeyCode.ESCAPE).executes(() -> {
            controller.closeInfoPanel();
            controller.getReplayTable().getSelectionModel().clearSelection();
        });
        registry.nodedKeybind(controller.getReplayTable(), KeyCode.A, KeyCombination.CONTROL_DOWN).executes(() -> controller.getReplayTable().getSelectionModel().selectAll());
        registry.nodedKeybind(controller.getReplayTable(), KeyCode.SPACE).or(KeyCode.ENTER).executes(() -> {
            var selectionModel = controller.getReplayTable().getSelectionModel();
            if (selectionModel.isEmpty()) {
                return;
            }

            selectionModel.getSelectedItems().forEach(e -> e.setLoaded(!e.isLoaded()));
        });
        registry.nodedKeybind(controller.getReplayTable(), KeyCode.DELETE).executes(() -> {
            var selection = controller.getReplayTable().getSelectionModel();
            if (selection.isEmpty()) {
                return;
            }

            ReplayBin displayed = controller.getBinEditor().getDisplayed();
            if (displayed == null || displayed.isGlobalBin()) {
                Toolkit.getDefaultToolkit().beep();
                return;
            }

            List<ReplayEntry> selected = new ArrayList<>(selection.getSelectedItems());
            selection.clearSelection();
            selected.forEach(r -> displayed.removeReplay(r.getReplay()));
            controller.closeInfoPanel();
        });

        registry.globalKeybind(KeyCode.F, KeyCombination.CONTROL_DOWN).executes(() -> controller.getFilterBar().requestFocus());
    }

}
