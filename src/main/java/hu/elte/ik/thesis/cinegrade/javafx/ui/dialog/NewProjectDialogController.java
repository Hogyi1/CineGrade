package hu.elte.ik.thesis.cinegrade.javafx.ui.dialog;

import hu.elte.ik.thesis.cinegrade.domain.enums.ErrorCode;
import hu.elte.ik.thesis.cinegrade.domain.enums.ViewType;
import hu.elte.ik.thesis.cinegrade.domain.error.ErrorHandler;
import hu.elte.ik.thesis.cinegrade.domain.exceptions.CineGradeException;
import hu.elte.ik.thesis.cinegrade.domain.navigation.NavigationManager;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BiConsumer;

public class NewProjectDialogController {

    private static final PseudoClass ERROR_PSEUDO = PseudoClass.getPseudoClass("error");
    private static Logger logger = LogManager.getLogger(NewProjectDialogController.class);
    private final BiConsumer<String, Path> createCatalogConsumer;
    private final NavigationManager navigationManager;
    private File selectedFolder;
    @FXML
    private TextField projectNameField;
    @FXML
    private TextField locationField;
    @FXML
    private Button browseButton;
    @FXML
    private Button cancelButton;
    @FXML
    private Button createButton;

    public NewProjectDialogController(BiConsumer<String, Path> createCatalogConsumer, Path basePath, NavigationManager navigationManager) {
        this.createCatalogConsumer = createCatalogConsumer;
        this.selectedFolder = basePath.toFile();
        this.navigationManager = navigationManager;
    }

    @FXML
    public void initialize() {
        setup();
    }

    private void setup() {
        projectNameField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue.trim().isEmpty()) {
                projectNameField.pseudoClassStateChanged(ERROR_PSEUDO, false);
                createButton.setDisable(false);
            } else {
                projectNameField.pseudoClassStateChanged(ERROR_PSEUDO, true);
                createButton.setDisable(true);
            }
        });

        createButton.setOnMouseClicked(e -> {
            boolean isValid = validateInput();
            if (isValid) {
                try {
                    createCatalogConsumer.accept(projectNameField.getText(), Path.of(selectedFolder.toURI()));
                    Stage stage = (Stage) cancelButton.getScene().getWindow();
                    stage.close();
                    // navigationManager.switchView(ViewType.TEST_MENU);
                    ErrorHandler.getInstance().handle(new CineGradeException(ErrorCode.UNKNOWN_ERROR, "Catalog created, database connection opened"));
                } catch (CineGradeException ex) {
                    ErrorHandler.getInstance().handle(ex);
                }
            } else {
                locationField.pseudoClassStateChanged(ERROR_PSEUDO, true);
                e.consume();
            }
        });

        cancelButton.setOnMouseClicked(e -> {
            Stage stage = (Stage) cancelButton.getScene().getWindow();
            stage.close();
        });

        locationField.setOnMouseClicked(e -> {
            openFileFolder();
        });
        locationField.editableProperty().set(false);
        locationField.setText(selectedFolder.toString());
        browseButton.setOnMouseClicked(e -> {
            openFileFolder();
        });
    }

    private void openFileFolder() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Open CineGrade project");
        if (selectedFolder != null && !selectedFolder.exists()) {
            selectedFolder.mkdirs();
        }
        if (selectedFolder != null && selectedFolder.isDirectory()) {
            directoryChooser.setInitialDirectory(selectedFolder);
        }
        Window mainWindow = cancelButton.getScene().getWindow();
        logger.debug("Default directory: {}", selectedFolder);
        File selectedFile = directoryChooser.showDialog(mainWindow);
        if (selectedFile != null) {
            this.selectedFolder = selectedFile;
            locationField.setText(selectedFolder.getAbsolutePath());
        }
    }

    private boolean validateInput() {
        if (selectedFolder != null && !selectedFolder.exists()) {
            selectedFolder.mkdirs();
        }
        return selectedFolder != null && selectedFolder.isDirectory() && !projectNameField.getText().trim().isBlank();
    }
}
