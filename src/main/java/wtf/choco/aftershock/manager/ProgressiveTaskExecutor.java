package wtf.choco.aftershock.manager;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import wtf.choco.aftershock.util.FXUtils;
import wtf.choco.aftershock.util.PublicTask;

import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ProgressiveTaskExecutor {

    private final Executor defaultExecutor;
    private final ProgressBar progressBar;
    private final Label statusLabel;

    public ProgressiveTaskExecutor(Executor defaultExecutor, ProgressBar progressBar, Label statusLabel) {
        this.defaultExecutor = defaultExecutor;
        this.progressBar = progressBar;
        this.statusLabel = statusLabel;
    }

    public <T> void execute(Task<T> task, BiConsumer<T, Worker.State> whenCompleted, Executor executor) {
        if (!progressBar.isVisible()) {
            this.progressBar.setVisible(true);
            this.statusLabel.setVisible(true);
        }

        DoubleProperty progressProperty = progressBar.progressProperty();
        progressProperty.unbind();
        progressProperty.bind(task.progressProperty());

        StringProperty messageProperty = statusLabel.textProperty();
        messageProperty.unbind();
        messageProperty.bind(task.messageProperty());

        task.setOnSucceeded(completionTask(whenCompleted, task::getValue));
        task.setOnFailed(completionTask(whenCompleted, () -> null)); // TODO: This hides exceptions! This is a problem!
        task.setOnCancelled(completionTask(whenCompleted, () -> null));

        executor.execute(task);
    }

    public <T> void execute(Consumer<PublicTask<T>> task, BiConsumer<T, Worker.State> whenCompleted, Executor executor) {
        this.execute(FXUtils.createTask(task), whenCompleted, executor);
    }

    public <T> void execute(Consumer<PublicTask<T>> task) {
        this.execute(FXUtils.createTask(task), null, defaultExecutor);
    }

    private <T> EventHandler<WorkerStateEvent> completionTask(BiConsumer<T, Worker.State> whenCompleted, Supplier<T> value) {
        return e -> {
            if (progressBar.isVisible()) {
                this.progressBar.setVisible(false);
                this.statusLabel.setVisible(false);
            }

            if (whenCompleted != null) {
                whenCompleted.accept(value.get(), e.getSource().getState());
            }
        };
    }

}
