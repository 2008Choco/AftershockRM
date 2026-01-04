package wtf.choco.aftershock.manager;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import wtf.choco.aftershock.util.FXUtils;
import wtf.choco.aftershock.util.PublicTask;
import wtf.choco.aftershock.util.function.ThrowingConsumer;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

public class ProgressiveTaskExecutor {

    public record TaskResult<T>(T result, Worker.State state) { }

    private final Executor defaultExecutor;
    private final ProgressBar progressBar;
    private final Label statusLabel;

    public ProgressiveTaskExecutor(Executor defaultExecutor, ProgressBar progressBar, Label statusLabel) {
        this.defaultExecutor = defaultExecutor;
        this.progressBar = progressBar;
        this.statusLabel = statusLabel;
    }

    public <T> CompletableFuture<TaskResult<T>> execute(Task<T> task, Executor executor) {
        if (!progressBar.isVisible()) {
            this.progressBar.setVisible(true);
            this.statusLabel.setVisible(true);
        }

        DoubleProperty progressProperty = progressBar.progressProperty();
        progressProperty.bind(task.progressProperty());

        StringProperty messageProperty = statusLabel.textProperty();
        messageProperty.unbind();
        messageProperty.bind(task.messageProperty());

        return CompletableFuture.runAsync(task, executor)
            .thenApplyAsync(ignore -> new TaskResult<>(task.getValue(), task.getState()), Platform::runLater)
            .whenComplete((_, _) -> {
                this.progressBar.setVisible(false);
                this.statusLabel.setVisible(false);
            });
    }

    public <T> CompletableFuture<TaskResult<T>> execute(ThrowingConsumer<PublicTask<T>> task, Executor executor) {
        return execute(FXUtils.createTask(task.toConsumer(CompletionException::new)), executor);
    }

    public <T> CompletableFuture<TaskResult<T>> execute(ThrowingConsumer<PublicTask<T>> task) {
        return execute(FXUtils.createTask(task.toConsumer(CompletionException::new)), defaultExecutor);
    }

}
