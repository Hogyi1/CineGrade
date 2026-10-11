package hu.elte.ik.thesis.cinegrade.javafx.ui.editing;

import hu.elte.ik.thesis.cinegrade.app.managers.editing.ImportManager;
import hu.elte.ik.thesis.cinegrade.domain.context.CatalogContext;
import hu.elte.ik.thesis.cinegrade.domain.editing.imports.StageEntry;
import hu.elte.ik.thesis.cinegrade.domain.enums.ErrorCode;
import hu.elte.ik.thesis.cinegrade.domain.enums.ImportMethod;
import hu.elte.ik.thesis.cinegrade.domain.enums.PhotoExtensions;
import hu.elte.ik.thesis.cinegrade.domain.error.ErrorHandler;
import hu.elte.ik.thesis.cinegrade.domain.exceptions.CineGradeException;
import hu.elte.ik.thesis.cinegrade.infra.utils.FormatUtils;
import hu.elte.ik.thesis.cinegrade.javafx.ui.PanelController;
import hu.elte.ik.thesis.cinegrade.javafx.ui.components.ImportPhotoCard;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Controller for the import panel used to stage and prepare photos before they are imported.
 * It handles file selection, drag-and-drop staging, card rendering, and selection state updates.
 */
public class ImportPanelController implements PanelController {

    private static final Logger logger = LogManager.getLogger(ImportPanelController.class);
    private final ImportManager importManager;
    private final ConcurrentHashMap<StageEntry, Parent> cardStageEntryMap = new ConcurrentHashMap<>();
    private NavigationController navigationController;
    private final Consumer<StageEntry> removeConsumer;

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
    private FlowPane cardsFlowPane;
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

    /**
     * Creates a new import panel controller bound to the supplied catalog context.
     *
     * @param catalogContext the catalog context containing the import manager and related state
     */
    public ImportPanelController(CatalogContext catalogContext) {
        this.importManager = catalogContext.importManager;

        removeConsumer = stageEntry -> {
            importManager.removeStageEntry(stageEntry);
            Parent root = cardStageEntryMap.remove(stageEntry);
            if (root != null) {
                cardsFlowPane.getChildren().remove(root);
            }
        };
    }

    /**
     * Initializes the import panel UI, including labels, buttons, slider configuration,
     * and drag-and-drop handling for staged files.
     */
    @FXML
    private void initialize() {
        logger.info("Initializing ImportPanelController");
        setupLabels();
        setupButtons();
        setupOthers();
        setupFlowPane();
        setupDropZone();
    }

    /**
     * Sets the navigation controller used to move between panels.
     *
     * @param navigationController the navigation controller to associate with the panel
     */
    @Override
    public void setNavigationController(NavigationController navigationController) {
        this.navigationController = navigationController;
    }

    /**
     * Indicates whether the current panel can safely be exited.
     *
     * @return {@code false} because exiting the import panel requires explicit cleanup of staged files
     */
    @Override
    public boolean canExit() {
        return false;
    }

    /**
     * Resets the staged import state when the user leaves the panel.
     */
    @Override
    public void onExit() {
        logger.info("Exiting ImportPanel, resetting staging area");
        cardStageEntryMap.values().forEach(root -> {
            cardsFlowPane.getChildren().remove(root);
        });
        cardStageEntryMap.clear();
        cardsFlowPane.getChildren().clear();
    }

    /**
     * Prepares the visual state of the panel when the user enters it.
     */
    @Override
    public void onEnter() {
        logger.info("Entered ImportPanel");
        cardsFlowPane.requestFocus();
        emptyStateContainer.setVisible(true);
        emptyStateContainer.setManaged(true);
    }

    /**
     * Sets up the buttons for the import panel.
     */
    private void setupButtons() {
        cancelButton.setOnMouseClicked(e -> {
            logger.info("Cancel button clicked, navigating to previous panel");

            onExit();
            importManager.resetAll();

            if (navigationController != null) {
                navigationController.switchToPreviousPanel();
            } else {
                logger.warn("navigationcontroller is null in importpanelcontroller");
            }
        });

        // Filters
        autoPairButton.setOnMouseClicked(e -> importManager.toggleAutoPair());
        duplicatesButton.setOnMouseClicked(e -> importManager.toggleHideDuplicates());

        autoPairCheckBox.selectedProperty().bindBidirectional(importManager.getAutoPairProperty());
        autoPairCheckBox.setDisable(true);
        autoPairCheckBox.setStyle("-fx-opacity: 1");

        autoPairButton.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                e.consume();
                autoPairCheckBox.setSelected(!autoPairCheckBox.isSelected());
            }
        });

        duplicatesCheckBox.selectedProperty().bindBidirectional(importManager.getHideDuplicatesProperty());
        duplicatesCheckBox.setDisable(true);
        duplicatesCheckBox.setStyle("-fx-opacity: 1");

        duplicatesButton.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                e.consume();
                duplicatesCheckBox.setSelected(!duplicatesCheckBox.isSelected());
            }
        });


        // Import button
        importButton.disableProperty().bind(
                Bindings.createBooleanBinding(
                        () -> importManager.getSelectedFilesProperty().get() == 0,
                        importManager.getSelectedFilesProperty()
                )
        );

        importButton.setOnMouseClicked(e -> {
            logger.info("Import button clicked");
            importManager.importPhotos(importMethodComboBox.getValue());
        });

        // Browse files button
        browseFilesButton.setOnMouseClicked(e -> openFileBrowser());

        // Selections
        selectAllButton.setOnMouseClicked(e -> {
            logger.debug("Select All clicked in ImportPanel");
            importManager.selectAll(true);
        });
        deselectAllButton.setOnMouseClicked(e -> {
            logger.debug("Deselect All clicked in ImportPanel");
            importManager.selectAll(false);
        });
        removeSelectedButton.setOnMouseClicked(e -> {
            logger.info("Remove selected clicked in IMportPanel");
            importManager.removeSelection();
        });
    }

    /**
     * Configures the drop zone to accept dragged photo files and stage them for import.
     * It updates the visual drag feedback and delegates file handling to the import manager.
     */
    private void setupDropZone() {

        // Accept files to be dropped
        dropZone.setOnDragOver(event -> {
            if (event.getGestureSource() != dropZone && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });

        // Show the drop zone when entering with files inhand
        dropZone.setOnDragEntered(event -> {
            if (event.getGestureSource() != dropZone && event.getDragboard().hasFiles()) {
                logger.debug("Drag entered dropZone with files");
                dropHint.setVisible(true);
            }
            event.consume();
        });

        // Hide on exit
        dropZone.setOnDragExited(event -> {
            logger.debug("Drag exited dropZone");
            dropHint.setVisible(false);
            event.consume();
        });

        // Accept files when dropped and hide the drop zone
        dropZone.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;

            if (db.hasFiles()) {
                success = true;
                List<File> files = db.getFiles();
                logger.info("Dropped {} file(s) onto dropZone", files.size());
                importManager.stagePhotos(files);
            } else {
                logger.warn("Drop gesture completed but Dragboard has no files");
            }

            event.setDropCompleted(success);
            dropHint.setVisible(false);
            event.consume();
        });
    }

    /**
     * Opens a file chooser so the user can select photo files to stage for import.
     */
    private void openFileBrowser() {
        logger.info("Import Photos button clicked, opening FileChooser");
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Photos");
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("All Files", PhotoExtensions.getAllWildcardPatterns()));
        Window mainWindow = browseFilesButton.getScene().getWindow();
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(mainWindow);
        if (selectedFiles != null && !selectedFiles.isEmpty()) {
            logger.info("Selected {} photo files from FileChooser", selectedFiles.size());
            try {
                importManager.stagePhotos(selectedFiles);
            } catch (Exception ex) {
                logger.error("Error staging photos from FileChooser", ex);
                ErrorHandler.getInstance().handle(ex);
            }
        } else {
            logger.debug("FileChooser cancelled or no files selected");
        }
    }

    /**
     * Configures the thumbnail zoom slider range and default scale.
     */
    private void setupOthers() {
        logger.debug("Configuring thumbnail zoom slider (0.4x to 1.4x)");
        thumbnailSizeSlider.setMin(0.4);
        thumbnailSizeSlider.setMax(1.4);
        thumbnailSizeSlider.setValue(0.9);

        logger.debug("Setting up search field");
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            importManager.getNameProperty().set(searchField.textProperty().get().trim());
        });

        logger.debug("Setting up combobox");
        importMethodComboBox.getItems().setAll(ImportMethod.values());
        importMethodComboBox.setValue(ImportMethod.ADD);
    }

    /**
     * Registers a listener that creates and removes photo cards as the staged entries change.
     */
    private void setupFlowPane() {
        logger.debug("Configuring cardsFlowPane reactive list listener");
        importManager.getFilteredEntries().addListener((ListChangeListener<StageEntry>) change -> {
            logger.debug("cardsFlowPane listener triggered: visible entries count = {}", change.getList().size());
            emptyStateContainer.setVisible(change.getList().isEmpty());
            emptyStateContainer.setManaged(change.getList().isEmpty());

            while (change.next()) {

                // There was a removal
                // List update happens in order: Remove -> Add
                // Soo when we modify the list it first shows the removed and then the added ones
                if (change.wasRemoved()) {
                    logger.debug("cardsFlowPane removing {} card(s)", change.getRemoved().size());
                    for (StageEntry entry : change.getRemoved()) {
                        Platform.runLater(() -> {
                            Parent cardNode = cardStageEntryMap.get(entry);
                            if (cardNode != null) {
                                cardNode.setManaged(false);
                                cardNode.setVisible(false);
                            }
                        });
                    }
                }

                if (change.wasAdded()) {
                    logger.debug("cardsFlowPane adding {} card(s)", change.getAddedSubList().size());
                    for (StageEntry entry : change.getAddedSubList()) {

                        // There is a cached card? Load that
                        if (cardStageEntryMap.containsKey(entry)) {
                            Platform.runLater(() -> {
                                Parent parent = cardStageEntryMap.get(entry);
                                parent.setManaged(true);
                                parent.setVisible(true);
                                logger.debug("Reusing existing card for staged entry: {}", entry.getFile() != null ? entry.getFile().getName() : "unknown");
                            });
                            continue;
                        }

                        // Load the card and cache it
                        logger.debug("Staged entry added: {}", entry.getFile() != null ? entry.getFile().getName() : "unknown");
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxmls/importPhotoCard.fxml"));
                        loader.setControllerFactory(clazz ->
                                new ImportPhotoCard(entry, removeConsumer));
                        try {
                            Parent root = loader.load();
                            ImportPhotoCard card = loader.getController();
                            card.bindSize(thumbnailSizeSlider.valueProperty());
                            cardStageEntryMap.put(entry, root);

                            Platform.runLater(() -> cardsFlowPane.getChildren().add(root));
                        } catch (IOException e) {
                            throw new CineGradeException(ErrorCode.REQ_RESOURCE_MISSING, e);
                        }
                    }
                }
            }
        });
    }

    /**
     * Binds the status labels and action button text to the current import manager state.
     */
    private void setupLabels() {
        logger.debug("Configuring reactive labels and format bindings");
        // Bind to selectedFileProperty
        selectedBadge.textProperty().bind(
                Bindings.createStringBinding(() -> String.format("%d Selected to Import", importManager.getSelectedFilesProperty().get()), importManager.getSelectedFilesProperty())
        );

        // Bind to selectedFileProperty
        importButton.textProperty().bind(
                Bindings.createStringBinding(() -> String.format("Import (%d)", importManager.getSelectedFilesProperty().get()), importManager.getSelectedFilesProperty())
        );

        // Bind to selectedFileProperty
        removeSelectedButton.textProperty().bind(
                Bindings.createStringBinding(() -> String.format("Remove (%d)", importManager.getSelectedFilesProperty().get()), importManager.getSelectedFilesProperty())
        );

        // Bind to stagedFilesProperty
        stagedCountLabel.textProperty().bind(
                Bindings.createStringBinding(() -> String.format("%d Photos Staged", importManager.getStagedFilesProperty().get()), importManager.getStagedFilesProperty())
        );

        // Bind to stageSizeProperty
        totalSizeLabel.setText("0 bytes total");
        totalSizeLabel.textProperty().bind(
                Bindings.createStringBinding(() -> String.format("%s total", FormatUtils.formatBytes(importManager.getStageSizeInBytes().longValue())), importManager.getStageSizeInBytes())
        );
    }
}
