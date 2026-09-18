package hu.elte.ik.thesis.cinegrade.javafx.ui.dialog;

import hu.elte.ik.thesis.cinegrade.domain.exceptions.CineGradeException;
import hu.elte.ik.thesis.cinegrade.infra.config.AppConfig;
import hu.elte.ik.thesis.cinegrade.javafx.ui.animations.TransitionFactory;
import javafx.animation.PauseTransition;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;

public class ErrorModalController {

    private static final Logger logger = LogManager.getLogger(ErrorModalController.class);
    private SequentialTransition copyTooltipTransition;

    @FXML
    private Label errorLabel;
    @FXML
    private TextArea errorMsgArea;
    @FXML
    private Button closeButton;
    @FXML
    private Button logButton;
    @FXML
    private Label copyLabel;

    @FXML
    private void initialize() {

        copyTooltipTransition = TransitionFactory.createFadePauseTransition(copyLabel, Duration.seconds(0.5), Duration.seconds(1.2), null);

        errorMsgArea.setOnMouseClicked(event -> {
            copyErrorToClipboard();
            showCopyTooltip();
        });

        logButton.setOnAction(e -> onOpenLogPressed());
        closeButton.setOnAction(e -> onCloseButtonPressed());
    }

    private void showCopyTooltip() {
        if (copyTooltipTransition.statusProperty().get() == Animation.Status.RUNNING) {
            return;
        }
        copyTooltipTransition.playFromStart();
    }

    private void copyErrorToClipboard() {

        String msg = errorLabel.getText() + "\n" + errorMsgArea.getText();

        StringSelection stringSelection = new StringSelection(msg);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
    }

    private void onCloseButtonPressed() {
        if (closeButton.getScene() != null && closeButton.getScene().getWindow() != null) {
            closeButton.getScene().getWindow().hide();
        }
    }

    private void onOpenLogPressed() {
        Path logDir = Path.of(AppConfig.INSTANCE.getLogDirectory());

        try {
            Optional<File> file = Arrays.stream(logDir.toFile().listFiles()).max(Comparator.comparing(File::getName));
            if (file.isPresent()) {
                Desktop.getDesktop().open(file.get());
            } else {
                Desktop.getDesktop().open(logDir.toFile());
            }
        } catch (Exception ex) {
            logger.error("Log file / log directory not found at path:  {}", logDir.toString());
        }
    }

    public void setupDialog(CineGradeException exception) {
        errorLabel.setText("%s - code: %d".formatted(exception.getErrorMessage(), exception.getErrorCodeValue()));

        Throwable cause = exception.getCause();

        StringWriter sw = new StringWriter();
        exception.printStackTrace(new PrintWriter(sw));

        String msg = sw.toString().trim().isEmpty() ? "No stack trace available." : sw.toString();
        errorMsgArea.setText(msg);
    }

    public void setupDialog(Exception exception) {
        errorLabel.setText("Unexpected error!");
        errorMsgArea.setText(exception.getLocalizedMessage());
    }
}
