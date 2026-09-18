package hu.elte.ik.thesis.cinegrade.app.main;

import hu.elte.ik.thesis.cinegrade.domain.enums.ErrorCode;
import hu.elte.ik.thesis.cinegrade.domain.exceptions.CineGradeException;
import hu.elte.ik.thesis.cinegrade.domain.results.ProcessResult;
import hu.elte.ik.thesis.cinegrade.infra.process.ProcessRunner;
import hu.elte.ik.thesis.cinegrade.infra.services.ffmpeg.FFmpegCommandBuilder;
import hu.elte.ik.thesis.cinegrade.javafx.ui.dialog.ErrorHandler;
import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class App extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        ErrorHandler.getInstance().install();

        // UI Controls
        Label label = new Label("CineGrade Diagnostic & Test Bench");
        label.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: -clr-text-primary;");

        Button themeToggleBtn = new Button("Toggle Light / Dark Theme");
        themeToggleBtn.getStyleClass().addAll("btn", "btn-secondary");

        Button uncaughtExceptionBtn = new Button("Throw Uncaught Exception (Generic)");
        uncaughtExceptionBtn.getStyleClass().addAll("btn", "btn-secondary");

        Button uncaughtExceptionBtn2 = new Button("Throw Uncaught CineGradeException");
        uncaughtExceptionBtn2.getStyleClass().addAll("btn", "btn-secondary");

        Button cinegradeException = new Button("Trigger Toast Warning (Delete Failed)");
        cinegradeException.getStyleClass().addAll("btn", "btn-secondary");

        Button cinegradeException2 = new Button("Trigger Toast Error (Write Failed)");
        cinegradeException2.getStyleClass().addAll("btn", "btn-secondary");

        Button cinegradeException3 = new Button("Trigger Fatal Error Modal (DB Schema Failed)");
        cinegradeException3.getStyleClass().addAll("btn", "btn-danger");

        // Center Content Layout
        VBox contentBox = new VBox(12, label, themeToggleBtn, uncaughtExceptionBtn, uncaughtExceptionBtn2,
                cinegradeException, cinegradeException2, cinegradeException3);
        contentBox.setAlignment(Pos.CENTER);
        contentBox.setPadding(new Insets(24));

        // Toast Container anchored to top-right
        VBox toastBox = new VBox(10);
        toastBox.setAlignment(Pos.TOP_RIGHT);
        toastBox.setPickOnBounds(false);

        // Root AnchorPane
        javafx.scene.layout.AnchorPane rootPane = new javafx.scene.layout.AnchorPane();
        rootPane.setStyle("-fx-background-color: -clr-almostdarkgrey;");
        rootPane.getStyleClass().add("theme-dark");

        javafx.scene.layout.AnchorPane.setTopAnchor(contentBox, 0.0);
        javafx.scene.layout.AnchorPane.setBottomAnchor(contentBox, 0.0);
        javafx.scene.layout.AnchorPane.setLeftAnchor(contentBox, 0.0);
        javafx.scene.layout.AnchorPane.setRightAnchor(contentBox, 0.0);

        javafx.scene.layout.AnchorPane.setTopAnchor(toastBox, 20.0);
        javafx.scene.layout.AnchorPane.setRightAnchor(toastBox, 20.0);

        rootPane.getChildren().addAll(contentBox, toastBox);

        ErrorHandler.getInstance().setToastContainer(toastBox);

        Scene scene = new Scene(rootPane, 720, 520);
        scene.getStylesheets().add(getClass().getResource("/css/theme.css").toExternalForm());

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

        primaryStage.setTitle("CineGrade Diagnostic Window");
        primaryStage.setScene(scene);
        primaryStage.show();

        try {
            FFmpegCommandBuilder builder = new FFmpegCommandBuilder();
            ProcessResult res = ProcessRunner.run(builder.versionInfo().buildCommands(), 1000, null);
            System.out.println("FFmpeg check: " + res.isSuccess());
        } catch (CineGradeException ex) {
            ErrorHandler.getInstance().handle(ex);
        }
    }
}
