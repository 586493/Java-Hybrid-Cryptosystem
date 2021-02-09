package hybrid.crypto.algorithms;

import hybrid.crypto.view.Main;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import lombok.NonNull;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static hybrid.crypto.view.Main.showDialog;

public class AsyncTask {
    private final AtomicBoolean showOnSucceeded = new AtomicBoolean(true);
    private final Task<Void> task;
    private final AtomicLong start;
    private final AtomicLong end;

    public void noAlertsOnSucceeded() {
        this.showOnSucceeded.set(false);
    }

    public AsyncTask(@NonNull final Callable<Void> toDo,
                     @NonNull final Callable<Void> onSucceededToDo,
                     @NonNull final Callable<Void> onFailedToDo) {
        this(
                toDo, onSucceededToDo, onFailedToDo,
                "Process completed successfully.",
                "An error occured!"
                );
    }

    public AsyncTask(@NonNull final Callable<Void> toDo,
                     @NonNull final Callable<Void> onSucceededToDo,
                     @NonNull final Callable<Void> onFailedToDo,
                     @NonNull final String successDialogText,
                     @NonNull final String failureDialogText) {

        start = new AtomicLong(0);
        end = new AtomicLong(0);

        task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                toDo.call();
                return null;
            }
        };
        task.exceptionProperty().addListener((observable, oldValue, newValue) ->  {
            if(newValue != null) {
                Exception ex = (Exception) newValue;
                ex.printStackTrace();
            }
        });
        task.setOnSucceeded(event1 -> {
            try {
                end.set(System.currentTimeMillis());
                onSucceededToDo.call();
            } catch (Exception e) {
                Main.errPrintln(e);
            }
            if(showOnSucceeded.get()) {
                showDialog(
                        successDialogText +
                                String.format(" [%d ms]", (end.get() - start.get())),
                        Alert.AlertType.INFORMATION);
            }
        });
        task.setOnFailed(event12 -> {
            try {
                end.set(System.currentTimeMillis());
                onFailedToDo.call();
            } catch (Exception e) {
                Main.errPrintln(e);
            }
            showDialog(
                    failureDialogText +
                            String.format(" [%d ms]", (end.get() - start.get())),
                    Alert.AlertType.ERROR);
        });
    }

    public void startNewThread() {
        final Thread thread = new Thread(task);
        start.set(System.currentTimeMillis());
        thread.start();
    }
}
