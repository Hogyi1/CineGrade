package hu.elte.ik.thesis.cinegrade.javafx.ui.dialog;

import hu.elte.ik.thesis.cinegrade.domain.enums.Severity;
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

        // Animate Entry (Slide in from right + Fade in)
        toastRoot.setOpacity(0.0);
        toastRoot.setTranslateX(40.0);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(250), toastRoot);
        fadeIn.setToValue(1.0);

        TranslateTransition slideIn = new TranslateTransition(Duration.millis(250), toastRoot);
        slideIn.setToX(0.0);

        ParallelTransition entryAnim = new ParallelTransition(fadeIn, slideIn);
        entryAnim.play();

        // Auto-dismiss after 4 seconds
        PauseTransition autoDismiss = new PauseTransition(Duration.seconds(4));
        autoDismiss.setOnFinished(e -> dismissWithAnimation());
        autoDismiss.play();
    }

    public void dismissWithAnimation() {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), toastRoot);
        fadeOut.setToValue(0.0);

        TranslateTransition slideOut = new TranslateTransition(Duration.millis(200), toastRoot);
        slideOut.setToX(50.0);

        ParallelTransition exitAnim = new ParallelTransition(fadeOut, slideOut);
        exitAnim.setOnFinished(e -> {
            if (onDismissCallback != null) {
                onDismissCallback.run();
            }
        });
        exitAnim.play();
    }

}