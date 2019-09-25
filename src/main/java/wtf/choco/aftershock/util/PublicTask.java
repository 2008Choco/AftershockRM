package wtf.choco.aftershock.util;

import javafx.concurrent.Task;

// Exists only to make the listed methods public rather than protected
public abstract class PublicTask<T> extends Task<T> {

    @Override
    public void updateMessage(String message) {
        super.updateMessage(message);
    }

    @Override
    public void updateProgress(double workDone, double max) {
        super.updateProgress(workDone, max);
    }

    @Override
    public void updateProgress(long workDone, long max) {
        super.updateProgress(workDone, max);
    }

    @Override
    public void updateTitle(String title) {
        super.updateTitle(title);
    }

    @Override
    public void updateValue(T value) {
        super.updateValue(value);
    }

}
