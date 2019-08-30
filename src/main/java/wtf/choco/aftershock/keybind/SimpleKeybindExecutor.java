package wtf.choco.aftershock.keybind;

import wtf.choco.aftershock.App;

@FunctionalInterface
public interface SimpleKeybindExecutor extends KeybindExecutor {

    public void accept();

    @Override
    public default void accept(App app) {
        this.accept();
    }

}
