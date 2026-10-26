// [AI-GENERATED]
package hu.elte.ik.thesis.cinegrade.javafx.ui.dialog;

import hu.elte.ik.thesis.cinegrade.javafx.ui.animations.TransitionFactory;
import javafx.animation.PauseTransition;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableDoubleValue;
import javafx.beans.value.ObservableStringValue;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * A toast for one running task, shown on the left side. Unlike the error toast it does not
 * auto-dismiss: it stays until {@link #finish(Outcome)} is called (or the user cancels it).
 * <p>
 * UI only: whoever creates it binds the task's properties via {@link #setupToast} and calls
 * {@link #finish} when the task reaches a final state.
 */
public class TaskToastController {

    /** How the task ended, decides the strip color and the final text. */
    public enum Outcome {
        SUCCEEDED("task-toast-succeeded", "Done"),
        FAILED("task-toast-failed", "Failed"),
        CANCELLED("task-toast-cancelled", "Cancelled");

        private final String styleClass;
        private final String label;

        Outcome(String styleClass, String label) {
            this.styleClass = styleClass;
            this.label = label;
        }
    }

    private static final Duration FINISHED_VISIBLE_FOR = Duration.seconds(2);

    @FXML
    private HBox toastRoot;
    @FXML
    private Label toastMessage;
    @FXML
    private ProgressBar toastProgressBar;
    @FXML
    private Label toastPercentage;
    @FXML
    private Button toastCancelButton;

    private Runnable onCancelCallback;
    private Runnable onDismissCallback;
    private boolean finished = false;

    @FXML
    private void initialize() {
        toastCancelButton.setOnAction(e -> {
            if (onCancelCallback != null) {
                onCancelCallback.run();
            }
        });
    }

    /**
     * Binds the toast to a task and slides it in.
     *
     * @param message   live status line (a Task's messageProperty)
     * @param progress  live progress 0..1, or -1 for indeterminate (a Task's progressProperty)
     * @param onCancel  called when the X is clicked (e.g. task::cancel); the toast stays until finish()
     * @param onDismiss called after the toast has animated out, remove the node from its container here
     */
    public void setupToast(ObservableStringValue message, ObservableDoubleValue progress,
                           Runnable onCancel, Runnable onDismiss) {
        this.onCancelCallback = onCancel;
        this.onDismissCallback = onDismiss;

        toastMessage.textProperty().bind(message);
        toastProgressBar.progressProperty().bind(progress);

        // Indeterminate progress (-1) shows the animated bar and hides the percentage
        toastPercentage.textProperty().bind(Bindings.createStringBinding(
                () -> progress.get() < 0 ? "" : "%d%%".formatted(Math.round(progress.get() * 100)),
                progress));

        TransitionFactory
                .createFadeSlideRight(toastRoot, Duration.millis(250), 40.0, false, null)
                .play();
    }

    /**
     * Switches the toast to its final state, then dismisses it after a short pause.
     * Safe to call more than once, only the first call counts.
     */
    public void finish(Outcome outcome) {
        if (finished) {
            return;
        }
        finished = true;

        toastProgressBar.progressProperty().unbind();
        toastPercentage.textProperty().unbind();
        toastCancelButton.setDisable(true);

        if (outcome == Outcome.SUCCEEDED) {
            toastProgressBar.setProgress(1.0);
        }
        toastPercentage.setText(outcome.label);
        toastRoot.getStyleClass().add(outcome.styleClass);

        PauseTransition pause = new PauseTransition(FINISHED_VISIBLE_FOR);
        pause.setOnFinished(e -> dismissWithAnimation());
        pause.play();
    }

    public void dismissWithAnimation() {
        TransitionFactory
                .createFadeSlideRight(toastRoot, Duration.millis(200), 50.0, true, () -> {
                    toastMessage.textProperty().unbind();
                    if (onDismissCallback != null) {
                        onDismissCallback.run();
                    }
                })
                .play();
    }
}
