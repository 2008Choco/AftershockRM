package wtf.choco.aftershock.keybind;

import java.util.function.Consumer;

import wtf.choco.aftershock.App;

@FunctionalInterface
public interface KeybindExecutor extends Consumer<App> { }
