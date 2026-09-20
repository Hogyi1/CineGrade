package hu.elte.ik.thesis.cinegrade.javafx.ui.dialog;

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

    private javafx.animation.Timeline progressTimeline;

    @FXML
    private void initialize() {
        if (progressBar != null) {
            progressBar.progressProperty().addListener((obs, oldVal, newVal) -> {
                updatePercentageText(newVal.doubleValue());
            });
        }
        setProgress(0.0, false);
    }

    public void setTaskName(String taskName) {
        if (taskNameLabel != null) {
            taskNameLabel.setText(taskName);
        }
    }

    public void setProgress(double progress) {
        setProgress(progress, true, null);
    }

    public void setProgress(double progress, boolean animated) {
        setProgress(progress, animated, null);
    }

    public void setProgress(double progress, boolean animated, Runnable onFinished) {
        if (progressBar == null) {
            if (onFinished != null) {
                onFinished.run();
            }
            return;
        }

        if (progressTimeline != null) {
            progressTimeline.stop();
        }

        if (!animated || progress < 0) {
            progressBar.setProgress(progress);
            updatePercentageText(progress);
            if (onFinished != null) {
                onFinished.run();
            }
            return;
        }

        double target = Math.min(1.0, Math.max(0.0, progress));
        double current = progressBar.getProgress();
        if (current < 0) {
            current = 0.0;
            progressBar.setProgress(0.0);
        }

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

        if (onFinished != null) {
            progressTimeline.setOnFinished(e -> onFinished.run());
        }

        progressTimeline.play();
    }

    private void updatePercentageText(double progress) {
        if (percentageLabel != null) {
            if (progress < 0) {
                percentageLabel.setText("");
            } else {
                int percent = (int) Math.round(Math.min(1.0, Math.max(0.0, progress)) * 100);
                percentageLabel.setText("[" + percent + "%]");
            }
        }
    }

    public void updateProgress(String taskName, double progress) {
        setTaskName(taskName);
        setProgress(progress);
    }

    public Label getTaskNameLabel() {
        return taskNameLabel;
    }

    public Label getPercentageLabel() {
        return percentageLabel;
    }

    public ProgressBar getProgressBar() {
        return progressBar;
    }

    public ImageView getRightsideImageView() {
        return rightsideImageView;
    }
}
