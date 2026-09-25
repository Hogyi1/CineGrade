package hu.elte.ik.thesis.cinegrade.javafx.ui.test;

import hu.elte.ik.thesis.cinegrade.domain.enums.ErrorCode;
import hu.elte.ik.thesis.cinegrade.domain.error.ErrorHandler;
import hu.elte.ik.thesis.cinegrade.domain.exceptions.CineGradeException;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;

public class ErrorTestController {

    @FXML
    private AnchorPane rootPane;

    @FXML
    private Button themeToggleBtn;

    @FXML
    private Button uncaughtExceptionBtn;

    @FXML
    private Button uncaughtExceptionBtn2;

    @FXML
    private Button cinegradeException;

    @FXML
    private Button cinegradeException2;

    @FXML
    private Button cinegradeException3;

    @FXML
    private void initialize() {
        themeToggleBtn.setOnAction(e -> {
            ObservableList<String> classes = rootPane.getStyleClass();
            if (classes.contains("theme-light")) {
                classes.remove("theme-light");
                classes.add("theme-dark");
            } else {
                classes.remove("theme-dark");
                classes.add("theme-light");
            }
        });

        uncaughtExceptionBtn.setOnMouseClicked(e -> {
            throw new RuntimeException("Generic unexpected runtime exception");
        });

        uncaughtExceptionBtn2.setOnMouseClicked(e -> {
            throw new CineGradeException(ErrorCode.DB_SCHEMA_INIT_FAILED);
        });

        cinegradeException.setOnMouseClicked(e -> {
            ErrorHandler.getInstance().handle(new CineGradeException(ErrorCode.FILE_DELETE_FAILED));
        });

        cinegradeException2.setOnMouseClicked(e -> {
            ErrorHandler.getInstance().handle(new CineGradeException(ErrorCode.FILE_WRITE_FAILED));
        });

        cinegradeException3.setOnMouseClicked(e -> {
            ErrorHandler.getInstance().handle(new CineGradeException(ErrorCode.DB_SCHEMA_INIT_FAILED));
        });
    }
}
