package hu.elte.ik.thesis.cinegrade.javafx.ui.animations;

import javafx.animation.*;
import javafx.scene.Node;
import javafx.util.Duration;

public final class TransitionFactory {

    private TransitionFactory() {
    }

    public static SequentialTransition createFadePauseTransition(Node node, Duration fadeDuration, Duration pauseDuration, Runnable onFinished) {
        FadeTransition fadeIn = new FadeTransition(fadeDuration, node);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        PauseTransition pause = new PauseTransition(pauseDuration);

        FadeTransition fadeOut = new FadeTransition(fadeDuration, node);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        SequentialTransition sequence = new SequentialTransition(fadeIn, pause, fadeOut);

        sequence.statusProperty().addListener((obs, oldStatus, newStatus) -> {
            if (newStatus == Animation.Status.RUNNING) {
                node.setVisible(true);
                node.setOpacity(0);
            }
        });

        sequence.setOnFinished(e -> {
            node.setVisible(false);
            node.setOpacity(1.0);
            if (onFinished != null) {
                onFinished.run();
            }
        });

        return sequence;
    }

    public static SequentialTransition createFadePauseTransition(Node node, Duration pauseDuration) {
        return createFadePauseTransition(node, Duration.millis(200), pauseDuration, null);
    }

    public static ParallelTransition createFadeSlideInTransition(Node node, Duration duration, double slideAmount, boolean fadeOut, Axis axis, Runnable onFinished) {
        FadeTransition fadeIn = new FadeTransition(duration, node);
        double fadeTarget = fadeOut ? 0.0 : 1.0;
        double fadeFrom = fadeOut ? 1.0 : 0.0;

        fadeIn.setFromValue(fadeFrom);
        fadeIn.setToValue(fadeTarget);

        TranslateTransition slideTransition = new TranslateTransition(duration, node);
        if (fadeOut) {
            // slideout
            switch (axis) {
                case X -> {
                    node.setTranslateX(0);
                    slideTransition.setFromX(0);
                    slideTransition.setToX(slideAmount);
                }
                case Y -> {
                    node.setTranslateY(0);
                    slideTransition.setFromY(0);
                    slideTransition.setToY(slideAmount);
                }
            }
        } else {
            // slidein
            switch (axis) {
                case X -> {
                    node.setTranslateX(slideAmount);
                    slideTransition.setFromX(slideAmount);
                    slideTransition.setToX(0);
                }
                case Y -> {
                    node.setTranslateY(slideAmount);
                    slideTransition.setFromY(slideAmount);
                    slideTransition.setToY(0);
                }
            }
        }
        ParallelTransition sequence = new ParallelTransition(fadeIn, slideTransition);

        sequence.statusProperty().addListener((obs, oldStatus, newStatus) -> {
            if (newStatus == Animation.Status.RUNNING) {
                node.setVisible(true);
                node.setOpacity(fadeFrom);
            }
        });

        sequence.setOnFinished(e -> {
            node.setVisible(!fadeOut);
            node.setOpacity(fadeTarget);
            if (onFinished != null) {
                onFinished.run();
            }
        });

        return sequence;
    }

    public static ParallelTransition createFadeSlideUp(Node node, Duration duration, double slideAmount, boolean fadeOut, Runnable onFinished) {
        return createFadeSlideInTransition(node, duration, slideAmount, fadeOut, Axis.Y, onFinished);
    }

    public static ParallelTransition createFadeSlideDown(Node node, Duration duration, double slideAmount, boolean fadeOut, Runnable onFinished) {
        return createFadeSlideInTransition(node, duration, slideAmount, fadeOut, Axis.Y, onFinished);
    }

    public static ParallelTransition createFadeSlideLeft(Node node, Duration duration, double slideAmount, boolean fadeOut, Runnable onFinished) {
        return createFadeSlideInTransition(node, duration, slideAmount, fadeOut, Axis.X, onFinished);
    }

    public static ParallelTransition createFadeSlideRight(Node node, Duration duration, double slideAmount, boolean fadeOut, Runnable onFinished) {
        return createFadeSlideInTransition(node, duration, -slideAmount, fadeOut, Axis.X, onFinished);
    }

    public enum Axis {
        X,
        Y;
    }
}
