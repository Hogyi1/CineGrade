package hu.elte.ik.thesis.cinegrade.javafx.ui.test;

import hu.elte.ik.thesis.cinegrade.domain.enums.ErrorCode;
import hu.elte.ik.thesis.cinegrade.domain.enums.ViewType;
import hu.elte.ik.thesis.cinegrade.domain.error.ErrorHandler;
import hu.elte.ik.thesis.cinegrade.domain.exceptions.CineGradeException;
import hu.elte.ik.thesis.cinegrade.domain.navigation.SceneManager;
import hu.elte.ik.thesis.cinegrade.domain.theme.ThemeManager;
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

    private SceneManager sceneManager;
    public ErrorTestController(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
    }

    @FXML
    private void initialize() {
        themeToggleBtn.setOnAction(e -> {
            ThemeManager.toggle();
        });

        uncaughtExceptionBtn.setOnMouseClicked(e -> {
            sceneManager.switchView(ViewType.PROJECT_MENU);
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
