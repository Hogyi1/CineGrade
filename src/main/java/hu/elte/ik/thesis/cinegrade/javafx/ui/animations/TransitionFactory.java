package hu.elte.ik.thesis.cinegrade.javafx.ui.animations;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
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
}
