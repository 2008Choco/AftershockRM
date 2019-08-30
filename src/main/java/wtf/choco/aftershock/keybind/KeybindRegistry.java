package wtf.choco.aftershock.keybind;

import java.util.ArrayList;
import java.util.List;

import wtf.choco.aftershock.App;

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
                if (keybind.combination.match(e) && keybind.action != null) {
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

    public boolean removeKeybind(KeyCode character, KeyCombination.Modifier... modifiers) {
        if (modifiers.length == 0) {
            return removeKeybind(character);
        }

        return keyActions.removeIf(a -> a.combination.equals(new KeyCodeCombination(character, modifiers)));
    }


    public final class KeybindAction {

        private KeybindExecutor action;
        private final KeyCombination combination;

        public KeybindAction(KeyCombination combination) {
            this.combination = combination;
        }

        public void executes(KeybindExecutor function) {
            this.action = function;
        }

        public void executes(SimpleKeybindExecutor function) {
            this.action = function;
        }

    }

}
