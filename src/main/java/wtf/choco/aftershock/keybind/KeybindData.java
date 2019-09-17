package wtf.choco.aftershock.keybind;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import wtf.choco.aftershock.App;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

public final class KeybindData {

    private KeybindExecutor action;
    private Predicate<Node> targetPredicate = (n) -> true;

    private final KeyCombination combination;
    private final List<KeyCombination> altCombinations;

    public KeybindData(KeyCombination combination) {
        this.combination = combination;
        this.altCombinations = new ArrayList<>(0);
    }

    public KeybindData executes(KeybindExecutor function) {
        this.action = function;
        return this;
    }

    public KeybindData executes(SimpleKeybindExecutor function) {
        this.action = function;
        return this;
    }

    public KeybindData specific(Predicate<Node> predicate) {
        this.targetPredicate = predicate;
        return this;
    }

    public KeybindData or(KeyCode character, KeyCombination.Modifier... modifiers) {
        this.altCombinations.add(new KeyCodeCombination(character, modifiers));
        return this;
    }

    boolean isValidTarget(Object target) {
        return targetPredicate.test(asNode(target));
    }

    boolean matchesEvent(KeyEvent event) {
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

    boolean matches(KeyCombination combination) {
        if (this.combination.equals(combination)) {
            return true;
        }

        for (KeyCombination altCombination : altCombinations) {
            if (altCombination.equals(combination)) {
                return true;
            }
        }

        return false;
    }

    void call(App app) {
        if (action != null) {
            this.action.accept(app);
        }
    }

    private Node asNode(Object target) {
        if (target instanceof Stage) {
            return ((Stage) target).getScene().getRoot();
        } else if (target instanceof Scene) {
            return ((Scene) target).getRoot();
        } else if (target instanceof Node) {
            return (Node) target;
        }

        return null;
    }

}