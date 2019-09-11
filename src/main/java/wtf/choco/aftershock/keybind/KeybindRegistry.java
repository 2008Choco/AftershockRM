package wtf.choco.aftershock.keybind;

import java.util.ArrayList;
import java.util.List;

import wtf.choco.aftershock.App;

import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

public final class KeybindRegistry {

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

}
