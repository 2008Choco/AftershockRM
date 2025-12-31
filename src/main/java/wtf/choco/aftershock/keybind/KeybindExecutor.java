package wtf.choco.aftershock.keybind;

import wtf.choco.aftershock.App;

import java.util.function.Consumer;

@FunctionalInterface
public interface KeybindExecutor extends Consumer<App> { }
