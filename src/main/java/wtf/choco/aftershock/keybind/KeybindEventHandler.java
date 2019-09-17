package wtf.choco.aftershock.keybind;

import java.util.ArrayList;
import java.util.List;

import wtf.choco.aftershock.App;

import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

public class KeybindEventHandler implements EventHandler<KeyEvent> {

    private final List<KeybindData> keybinds = new ArrayList<>();
    private final App app;

    protected KeybindEventHandler(App app, Node node) {
        this.app = app;

        if (node != null) {
            node.setOnKeyPressed(this);
        }
    }

    public void addKeybind(KeybindData data) {
        this.keybinds.add(data);
    }

    public boolean removeKeybind(KeyCode character, KeyCombination.Modifier... modifiers) {
        KeyCombination combination = new KeyCodeCombination(character, modifiers);
        return keybinds.removeIf(a -> a.matches(combination));
    }

    public void clearKeybinds() {
        this.keybinds.clear();
    }

    @Override
    public void handle(KeyEvent event) {
        if (keybinds.isEmpty()) {
            return;
        }

        Object source = event.getSource();

        for (KeybindData keybind : keybinds) {
            if (!keybind.matchesEvent(event) || !keybind.isValidTarget(source)) {
                continue;
            }

            keybind.call(app);
        }
    }

}
