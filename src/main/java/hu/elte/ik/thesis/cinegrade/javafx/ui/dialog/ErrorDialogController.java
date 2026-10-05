package hu.elte.ik.thesis.cinegrade.javafx.ui.dialog;

import hu.elte.ik.thesis.cinegrade.domain.enums.Severity;
import hu.elte.ik.thesis.cinegrade.javafx.ui.animations.TransitionFactory;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class ErrorDialogController {

    @FXML
    private HBox toastRoot;
    @FXML
    private Rectangle statusIndicator;
    @FXML
    private Label toastTitle;
    @FXML
    private Label toastMessage;
    @FXML
    private Button toastCloseButton;

    private Runnable onDismissCallback;

    @FXML
    private void initialize() {
        toastCloseButton.setOnAction(e -> dismissWithAnimation());
    }

    public void setupToast(String title, String message, Severity severity, Runnable onDismiss) {
        this.onDismissCallback = onDismiss;
        toastTitle.setText(title);
        toastMessage.setText(message);

        if (severity == Severity.ERROR) {
            statusIndicator.setFill(Color.web("#e02424")); // Red
            toastTitle.setTextFill(Color.web("#ff5555"));
        } else {
            statusIndicator.setFill(Color.web("#f59e0b")); // Amber / Yellow
            toastTitle.setTextFill(Color.web("#fbbf24"));
        }

        ParallelTransition transition = TransitionFactory
                .createFadeSlideLeft(toastRoot, Duration.millis(250), 40.0, false, null);
        transition.play();

        // Auto-dismiss after 4 seconds
        PauseTransition autoDismiss = new PauseTransition(Duration.seconds(4));
        autoDismiss.setOnFinished(e -> dismissWithAnimation());
        autoDismiss.play();
    }

    public void dismissWithAnimation() {
        ParallelTransition transition = TransitionFactory
                .createFadeSlideRight(toastRoot, Duration.millis(200), 50.0, true, () -> {
                    if (onDismissCallback != null) {
                        onDismissCallback.run();
                    }
                });
        transition.play();
    }

}