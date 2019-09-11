package wtf.choco.aftershock.keybind;

import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.List;

import wtf.choco.aftershock.App;
import wtf.choco.aftershock.controller.AppController;
import wtf.choco.aftershock.manager.BinRegistry;
import wtf.choco.aftershock.structure.ReplayBin;
import wtf.choco.aftershock.structure.ReplayEntry;

import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

public final class KeybindRegistry {

    private boolean registeredDefaults = false;

    private final List<KeybindAction> keyActions = new ArrayList<>();
    private final App app;

    public KeybindRegistry(App app) {
        this.app = app;
        this.app.getStage().addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            for (KeybindAction keybind : keyActions) {
                if (keybind.matchesEvent(e) && keybind.action != null) {
                    keybind.action.accept(app);
                }
            }
        });
    }

    public KeybindAction registerKeybind(KeyCode character, KeyCombination.Modifier... modifiers) {
        KeybindAction keybind = new KeybindAction(new KeyCodeCombination(character, modifiers));
        this.keyActions.add(keybind);
        return keybind;
    }

    public KeybindAction registerKeybind(Node parent, KeyCode character, KeyCombination.Modifier... modifiers) {
        KeybindAction keybind = new KeybindAction(new KeyCodeCombination(character, modifiers));
        parent.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (keybind.matchesEvent(e) && keybind.action != null) {
                keybind.action.accept(app);
            }
        });

        return keybind;
    }

    public boolean removeKeybind(KeyCode character, KeyCombination.Modifier... modifiers) {
        if (modifiers.length == 0) {
            return removeKeybind(character);
        }

        return keyActions.removeIf(a -> a.combination.equals(new KeyCodeCombination(character, modifiers)));
    }


    public final class KeybindAction {

        private KeybindExecutor action;
        private final KeyCombination combination;
        private final List<KeyCombination> altCombinations;

        public KeybindAction(KeyCombination combination) {
            this.combination = combination;
            this.altCombinations = new ArrayList<>(0);
        }

        public void executes(KeybindExecutor function) {
            this.action = function;
        }

        public void executes(SimpleKeybindExecutor function) {
            this.action = function;
        }

        public KeybindAction or(KeyCode character, KeyCombination.Modifier... modifiers) {
            this.altCombinations.add(new KeyCodeCombination(character, modifiers));
            return this;
        }

        private boolean matchesEvent(KeyEvent event) {
            if (combination.match(event)) {
                return true;
            }

            for (KeyCombination combination : altCombinations) {
                if (combination.match(event)) {
                    return true;
                }
            }

            return false;
        }

    }

    public static void registerDefaultKeybinds(KeybindRegistry registry) {
        if (registry.registeredDefaults) {
            throw new IllegalStateException("Cannot register default keybinds twice");
        }

        AppController controller = registry.app.getController();

        registry.registerKeybind(KeyCode.ESCAPE).executes(() -> {
            controller.closeInfoPanel();
            controller.getReplayTable().getSelectionModel().clearSelection();
        });
        registry.registerKeybind(controller.getReplayTable(), KeyCode.A, KeyCombination.CONTROL_DOWN).executes(() -> controller.getReplayTable().getSelectionModel().selectAll());
        registry.registerKeybind(controller.getReplayTable(), KeyCode.SPACE).or(KeyCode.ENTER).executes(() -> {
            var selectionModel = controller.getReplayTable().getSelectionModel();
            if (selectionModel.isEmpty()) {
                return;
            }

            selectionModel.getSelectedItems().forEach(e -> e.setLoaded(!e.isLoaded()));
        });
        registry.registerKeybind(controller.getReplayTable(), KeyCode.DELETE).executes(() -> {
            var selection = controller.getReplayTable().getSelectionModel();
            if (selection.isEmpty()) {
                return;
            }

            ReplayBin displayed = controller.getBinEditor().getDisplayed();
            if (displayed == BinRegistry.GLOBAL_BIN) {
                Toolkit.getDefaultToolkit().beep();
                return;
            }

            List<ReplayEntry> selected = new ArrayList<>(selection.getSelectedItems());
            selection.clearSelection();
            selected.forEach(r -> displayed.removeReplay(r.getReplay()));
            controller.closeInfoPanel();
        });

        registry.registerKeybind(KeyCode.F, KeyCombination.CONTROL_DOWN).executes(() -> controller.getFilterBar().requestFocus());
    }

}
