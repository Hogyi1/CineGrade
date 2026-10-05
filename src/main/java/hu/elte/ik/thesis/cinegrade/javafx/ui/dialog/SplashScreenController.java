package hu.elte.ik.thesis.cinegrade.javafx.ui.dialog;

import hu.elte.ik.thesis.cinegrade.domain.tasks.AppInitService;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

public class SplashScreenController {

    @FXML
    private Label taskNameLabel;

    @FXML
    private Label percentageLabel;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private ImageView rightsideImageView;

    private Timeline progressTimeline;
    private final AppInitService appInitService;

    public SplashScreenController(AppInitService appInitService) {
        this.appInitService = appInitService;
    }

    @FXML
    private void initialize() {
        if (progressBar != null) {
            progressBar.progressProperty().addListener((obs, oldVal, newVal) -> {
                updatePercentageText(newVal.doubleValue());
            });
        }
        setProgress(0.0);

        if (appInitService != null) {
            appInitService.progressProperty().addListener((obs, oldVal, newVal) -> {
                setProgress(newVal.doubleValue());
            });
            taskNameLabel.textProperty().bind(appInitService.messageProperty());
        }
    }

    private void setProgress(double progress) {
                if (progressTimeline != null) {
            progressTimeline.stop();
        }

        double target = Math.clamp(progress, 0.0, 1.0);
        double current = Math.clamp(progressBar.getProgress(), 0.0, 1.0);

        progressTimeline = new Timeline(
            new KeyFrame(
                Duration.ZERO,
                new KeyValue(progressBar.progressProperty(), current)
            ),
            new KeyFrame(
                Duration.millis(350),
                new KeyValue(progressBar.progressProperty(), target, Interpolator.EASE_BOTH)
            )
        );

        progressTimeline.play();
    }

    /**
     * Update the percentage label
     * */
    private void updatePercentageText(double progress) {
        if (percentageLabel != null) {
            if (progress <= 0) {
                percentageLabel.setText("[0%]");
            } else {
                int percent = (int) Math.round(Math.clamp(progress, 0.0, 1.0) * 100);
                percentageLabel.setText("[" + percent + "%]");
            }
        }
    }

    /**
     * Start the service
     * */
    public void startService() {
        if (appInitService != null && !appInitService.isRunning()) {
            appInitService.start();
        }
    }
}
