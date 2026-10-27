package hu.elte.ik.thesis.cinegrade.javafx.ui.editing;

import hu.elte.ik.thesis.cinegrade.app.managers.imports.ImportManager;
import hu.elte.ik.thesis.cinegrade.app.managers.imports.StagingArea;
import hu.elte.ik.thesis.cinegrade.domain.editing.imports.StageEntry;
import hu.elte.ik.thesis.cinegrade.domain.enums.ImportMethod;
import hu.elte.ik.thesis.cinegrade.domain.enums.PhotoExtensions;
import hu.elte.ik.thesis.cinegrade.domain.error.ErrorHandler;
import hu.elte.ik.thesis.cinegrade.infra.utils.format.FormatUtils;
import hu.elte.ik.thesis.cinegrade.javafx.ui.components.ImportPhotoCard;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.control.GridView;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

/**
 * Controller for the import panel used to stage and prepare photos before import.
 */
public class ImportPanelController implements PanelController {

    private static final Logger logger = LogManager.getLogger(ImportPanelController.class);
    private final ImportManager importManager;
    private final StagingArea stagingArea;
    private NavigationController navigationController;
    private final Consumer<StageEntry> removeConsumer;

    private static final double BASE_CARD_WIDTH = 260.0;
    private static final double THUMBNAIL_ASPECT_RATIO = 1.5; // 3:2
    private static final double FOOTER_HEIGHT = 68.0;

    @FXML
    private Label stagedCountLabel;
    @FXML
    private Label selectedBadge;
    @FXML
    private Label totalSizeLabel;
    @FXML
    private Button selectAllButton;
    @FXML
    private Button deselectAllButton;
    @FXML
    private Slider thumbnailSizeSlider;
    @FXML
    private VBox dropZone;
    @FXML
    private VBox dropHint;
    @FXML
    private VBox emptyStateContainer;
    @FXML
    private GridView<StageEntry> cardsGridView;
    @FXML
    private Button browseFilesButton;
    @FXML
    private Button cancelButton;
    @FXML
    private Button importButton;
    @FXML
    private Button autoPairButton;
    @FXML
    private Button duplicatesButton;
    @FXML
    private Button removeSelectedButton;
    @FXML
    private TextField searchField;
    @FXML
    private CheckBox autoPairCheckBox;
    @FXML
    private CheckBox duplicatesCheckBox;
    @FXML
    private ComboBox<ImportMethod> importMethodComboBox;
    @FXML
    private VBox stageProgressOverlay;


    public ImportPanelController(ImportManager importManager, StagingArea stagingArea) {
        this.importManager = importManager;
        this.stagingArea = stagingArea;
        this.removeConsumer = stagingArea::removeStageEntry;
    }

    @FXML
    private void initialize() {
        logger.info("Initializing ImportPanelController");
        setupLabels();
        setupButtons();
        setupOthers();
        setupGridView();
        setupDropZone();
    }

    @Override
    public void setNavigationController(NavigationController navigationController) {
        this.navigationController = navigationController;
    }

    @Override
    public boolean canExit() {
        return false;
    }

    @Override
    public void onExit() {
        logger.info("Exiting ImportPanel");
    }

    @Override
    public void onEnter() {
        logger.info("Entered ImportPanel");
        cardsGridView.requestFocus();
        resetPanel();
    }

    private void resetPanel() {
        setupOthers();
    }

    // [AI-GENERATED METHOD]
    private void setupGridView() {
        logger.debug("Configuring cardsGridView");

        // Set spacing between cells
        cardsGridView.setHorizontalCellSpacing(12.0);
        cardsGridView.setVerticalCellSpacing(12.0);

        // Bind data and cell factory
        cardsGridView.setItems(stagingArea.getFilteredEntries());
        cardsGridView.setCellFactory(view -> new ImportPhotoCard(removeConsumer, cardsGridView));

        // Initial sizing
        double initialWidth = BASE_CARD_WIDTH;
        double initialHeight = Math.round((initialWidth / THUMBNAIL_ASPECT_RATIO) + FOOTER_HEIGHT);
        cardsGridView.setCellWidth(initialWidth);
        cardsGridView.setCellHeight(initialHeight);

        // Dynamic Cell Sizing via Slider (smooth & quantized, prevents layout thrashing)
        thumbnailSizeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            double scale = newVal.doubleValue();
            double targetWidth = Math.round(scale * BASE_CARD_WIDTH);
            if (Math.abs(targetWidth - cardsGridView.getCellWidth()) >= 2.0) {
                double targetHeight = Math.round((targetWidth / THUMBNAIL_ASPECT_RATIO) + FOOTER_HEIGHT);
                cardsGridView.setCellWidth(targetWidth);
                cardsGridView.setCellHeight(targetHeight);
            }
        });

        // Reactive Empty State Overlay
        emptyStateContainer.visibleProperty().bind(Bindings.isEmpty(cardsGridView.getItems()));
        emptyStateContainer.managedProperty().bind(emptyStateContainer.visibleProperty());
    }

    private void setupOthers() {
        logger.debug("Configuring thumbnail zoom slider (0.45x to 1.5x)");
        thumbnailSizeSlider.setMin(0.45);
        thumbnailSizeSlider.setMax(1.5);
        thumbnailSizeSlider.setValue(1.5 - 0.45);
        thumbnailSizeSlider.setBlockIncrement(0.05);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            stagingArea.getNameProperty().set(newVal.trim());
        });

        importMethodComboBox.getItems().setAll(ImportMethod.values());
        importMethodComboBox.setValue(ImportMethod.ADD);
    }

    private void setupButtons() {
        cancelButton.setOnMouseClicked(e -> {
            logger.info("Cancel button clicked");
            onExit();
            importManager.resetAll();
            if (navigationController != null) {
                navigationController.switchToPreviousPanel();
            }
        });

        autoPairButton.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                e.consume();
                autoPairCheckBox.setSelected(!autoPairCheckBox.isSelected());
            }
        });
        autoPairCheckBox.selectedProperty().bindBidirectional(stagingArea.getAutoPairProperty());
        autoPairCheckBox.setDisable(true);
        autoPairCheckBox.setStyle("-fx-opacity: 1");

        duplicatesButton.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                e.consume();
                duplicatesCheckBox.setSelected(!duplicatesCheckBox.isSelected());
            }
        });
        duplicatesCheckBox.selectedProperty().bindBidirectional(stagingArea.getHideDuplicatesProperty());
        duplicatesCheckBox.setDisable(true);
        duplicatesCheckBox.setStyle("-fx-opacity: 1");

        if (stageProgressOverlay != null) {
            stageProgressOverlay.visibleProperty().bind(stagingArea.getStagingProperty());
            stageProgressOverlay.managedProperty().bind(stagingArea.getStagingProperty());
        }

        importButton.disableProperty().bind(
                stagingArea.getStagingProperty().or(
                        Bindings.createBooleanBinding(
                                () -> stagingArea.getSelectedFilesProperty().get() == 0,
                                stagingArea.getSelectedFilesProperty()
                        )
                )
        );
        importButton.setOnMouseClicked(e -> importManager.importPhotos(importMethodComboBox.getValue()));

        cancelButton.disableProperty().bind(stagingArea.getStagingProperty());
        selectAllButton.disableProperty().bind(stagingArea.getStagingProperty());
        deselectAllButton.disableProperty().bind(stagingArea.getStagingProperty());
        removeSelectedButton.disableProperty().bind(stagingArea.getStagingProperty());

        browseFilesButton.setOnMouseClicked(e -> openFileBrowser());
        selectAllButton.setOnMouseClicked(e -> stagingArea.selectAll(true));
        deselectAllButton.setOnMouseClicked(e -> stagingArea.selectAll(false));
        removeSelectedButton.setOnMouseClicked(e -> stagingArea.removeSelection());
    }

    private void setupDropZone() {
        dropZone.setOnDragOver(event -> {
            if (event.getGestureSource() != dropZone && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });

        dropZone.setOnDragEntered(event -> {
            if (event.getGestureSource() != dropZone && event.getDragboard().hasFiles()) {
                dropHint.setVisible(true);
            }
            event.consume();
        });

        dropZone.setOnDragExited(event -> {
            dropHint.setVisible(false);
            event.consume();
        });

        dropZone.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                success = true;
                List<File> files = db.getFiles();
                importManager.stagePhotos(files);
            }
            event.setDropCompleted(success);
            dropHint.setVisible(false);
            event.consume();
        });
    }

    private void openFileBrowser() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Photos");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("All Files", PhotoExtensions.getAllWildcardPatterns())
        );
        Window mainWindow = browseFilesButton.getScene().getWindow();
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(mainWindow);
        if (selectedFiles != null && !selectedFiles.isEmpty()) {
            try {
                importManager.stagePhotos(selectedFiles);
            } catch (Exception ex) {
                logger.error("Error staging photos from FileChooser", ex);
                ErrorHandler.getInstance().handle(ex);
            }
        }
    }

    private void setupLabels() {
        selectedBadge.textProperty().bind(
                Bindings.createStringBinding(() -> "%d Selected to Import".formatted(stagingArea.getSelectedFilesProperty().get()),
                        stagingArea.getSelectedFilesProperty())
        );
        importButton.textProperty().bind(
                Bindings.createStringBinding(() -> "Import (%d)".formatted(stagingArea.getSelectedFilesProperty().get()),
                        stagingArea.getSelectedFilesProperty())
        );
        removeSelectedButton.textProperty().bind(
                Bindings.createStringBinding(() -> "Remove (%d)".formatted(stagingArea.getSelectedFilesProperty().get()),
                        stagingArea.getSelectedFilesProperty())
        );
        stagedCountLabel.textProperty().bind(
                Bindings.createStringBinding(() -> "%d Photos Staged".formatted(stagingArea.getStagedFilesProperty().get()),
                        stagingArea.getStagedFilesProperty())
        );
        totalSizeLabel.textProperty().bind(
                Bindings.createStringBinding(() -> "%s total".formatted(FormatUtils.formatBytes(stagingArea.getStageSizeInBytes().longValue())),
                        stagingArea.getStageSizeInBytes())
        );
    }
}