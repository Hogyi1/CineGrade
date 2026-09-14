package hu.elte.ik.thesis.cinegrade.javafx.ui.dialog;

import hu.elte.ik.thesis.cinegrade.domain.exceptions.CineGradeException;
import hu.elte.ik.thesis.cinegrade.infra.config.AppConfig;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;

public class ErrorModalController {

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
        errorMsgArea.setOnMouseClicked(event -> {
            copyErrorToClipboard();
            showCopyTooltip();
        });

        logButton.setOnAction(e -> onOpenLogPressed());
        closeButton.setOnAction(e -> onCloseButtonPressed());
    }

    private void showCopyTooltip() {
        copyLabel.setVisible(true);
        PauseTransition pause = new PauseTransition(Duration.seconds(1.2));
        pause.setOnFinished(e -> copyLabel.setVisible(false));
        pause.play();
    }

    private void copyErrorToClipboard() {
        StringBuilder builder = new StringBuilder();

        builder.append(errorLabel.getText());
        builder.append("\n");
        builder.append(errorMsgArea.getText());

        StringSelection stringSelection = new StringSelection(builder.toString());
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
    }

    private void onCloseButtonPressed() {
        Platform.runLater(Platform::exit);
    }

    private void onOpenLogPressed() {
        Path logDir = Path.of(AppConfig.INSTANCE.getLogDirectory());
        File[] files = logDir.toFile().listFiles(pathname -> pathname.toString().contains("latest"));

        try {
            if (files != null && files.length > 0 && files[0].exists()) {
                Desktop.getDesktop().open(files[0]);
            } else {
                Desktop.getDesktop().open(logDir.toFile());
            }
        } catch (IOException ex){
            return;
        }
    }

    public void setupDialog(CineGradeException exception) {
        errorLabel.setText("%s - code: %d".formatted(exception.getErrorMessage(), exception.getErrorCodeValue()));

        Throwable cause = exception.getCause();

        StringWriter sw = new StringWriter();
        exception.printStackTrace(new PrintWriter(sw));

        String msg = cause != null ? sw.toString() : "No stack trace";
        errorMsgArea.setText(msg);
    }

    public void setupDialog(Exception exception) {
        errorLabel.setText("Unexpected error!");
        errorMsgArea.setText(exception.getLocalizedMessage());
    }
}
