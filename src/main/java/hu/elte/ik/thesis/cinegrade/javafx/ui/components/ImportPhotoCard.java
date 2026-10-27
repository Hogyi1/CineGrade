package hu.elte.ik.thesis.cinegrade.javafx.ui.components;

import hu.elte.ik.thesis.cinegrade.domain.editing.imports.StageEntry;
import hu.elte.ik.thesis.cinegrade.domain.enums.ErrorCode;
import hu.elte.ik.thesis.cinegrade.domain.exceptions.CineGradeException;
import hu.elte.ik.thesis.cinegrade.infra.utils.format.FormatUtils;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.control.GridCell;
import org.controlsfx.control.GridView;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Controller and GridCell representation for a staged photo in ControlsFX GridView.
 */
public class ImportPhotoCard extends GridCell<StageEntry> {

    private static final Logger logger = LogManager.getLogger(ImportPhotoCard.class);
    private static final double FOOTER_HEIGHT = 68.0;

    @FXML
    private VBox cardRoot;

    @FXML
    private StackPane thumbnailContainer;

    @FXML
    private ImageView thumbnailImageView;

    @FXML
    private CheckBox selectCheckBox;

    @FXML
    private Label fileNameLabel;

    @FXML
    private Label fileSizeLabel;

    @FXML
    private Label metaLabel;

    @FXML
    private Label formatBadge;

    @FXML
    private Label duplicateBadge;

    private final Consumer<StageEntry> removeAction;
    private final GridView<StageEntry> parentGridView;
    private ContextMenu optionsMenu;

    private ChangeListener<Boolean> selectionListener;
    private ChangeListener<Boolean> duplicateListener;
    private StageEntry currentEntry;

    public ImportPhotoCard() {
        this(null, null);
    }

    public ImportPhotoCard(Consumer<StageEntry> removeAction) {
        this(removeAction, null);
    }

    public ImportPhotoCard(Consumer<StageEntry> removeAction, org.controlsfx.control.GridView<StageEntry> gridView) {
        this.removeAction = removeAction;
        this.parentGridView = gridView;

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxmls/importPhotoCard.fxml"));
        loader.setController(this);

        try {
            loader.load();
        } catch (IOException exception) {
            logger.error("Failed to load /fxmls/importPhotoCard.fxml", exception);
            throw new CineGradeException(ErrorCode.REQ_RESOURCE_MISSING, "/fxmls/importPhotoCard.fxml");
        }

        setupCellLayout();
        setupOptionsMenu();
    }

    // [AI-GENERATED METHOD]
    private void setupCellLayout() {
        // Keep CheckBox visual only; card click controls selection state
        selectCheckBox.setDisable(true);
        selectCheckBox.setStyle("-fx-opacity: 1;");

        cardRoot.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                e.consume();
                if (currentEntry != null) {
                    currentEntry.getIsSelectedProperty().set(!currentEntry.getIsSelectedProperty().get());
                }
            }
        });

        if (parentGridView != null) {
            // Strictly bind cardRoot and thumbnailContainer to the GridView cell dimensions
            cardRoot.prefWidthProperty().bind(parentGridView.cellWidthProperty().subtract(4));
            cardRoot.prefHeightProperty().bind(parentGridView.cellHeightProperty().subtract(4));
            cardRoot.maxWidthProperty().bind(parentGridView.cellWidthProperty().subtract(4));
            cardRoot.maxHeightProperty().bind(parentGridView.cellHeightProperty().subtract(4));

            thumbnailContainer.prefHeightProperty().bind(parentGridView.cellHeightProperty().subtract(FOOTER_HEIGHT + 4));
            thumbnailContainer.minHeightProperty().bind(parentGridView.cellHeightProperty().subtract(FOOTER_HEIGHT + 4));
            thumbnailContainer.maxHeightProperty().bind(parentGridView.cellHeightProperty().subtract(FOOTER_HEIGHT + 4));
        } else {
            cardRoot.setMaxWidth(Double.MAX_VALUE);
            cardRoot.setMaxHeight(Double.MAX_VALUE);
        }
    }

    @Override
    protected void updateItem(StageEntry item, boolean empty) {
        super.updateItem(item, empty);

        // Clean up previous item bindings to prevent memory leaks and ghost toggles during scrolling
        if (currentEntry != null) {
            selectCheckBox.selectedProperty().unbindBidirectional(currentEntry.getIsSelectedProperty());
            if (selectionListener != null) {
                currentEntry.getIsSelectedProperty().removeListener(selectionListener);
            }
            if (duplicateListener != null) {
                currentEntry.getIsMaybeDuplicateProperty().removeListener(duplicateListener);
            }
            currentEntry = null;
        }

        if (empty || item == null) {
            setGraphic(null);
            setText(null);
            thumbnailImageView.setImage(null);
            if (optionsMenu != null && optionsMenu.isShowing()) {
                optionsMenu.hide();
            }
            return;
        }

        currentEntry = item;
        setupCard(item);
        setGraphic(cardRoot);
    }

    // [AI-GENERATED METHOD]
    private void setupCard(StageEntry stageEntry) {
        fileSizeLabel.setText(FormatUtils.formatBytes(stageEntry.size));
        fileNameLabel.setText(stageEntry.getFile() != null ? stageEntry.getFile().getName() : "");
        String fileType = stageEntry.isRaw ? (stageEntry.jpegPath != null ? "[RAW + JPEG]" : "RAW") : stageEntry.fileType;
        metaLabel.setText("%s • %s".formatted(stageEntry.imageSize, fileType));
        formatBadge.setText(stageEntry.fileType);

        // Bind selection state
        selectCheckBox.selectedProperty().bindBidirectional(stageEntry.getIsSelectedProperty());
        selectionListener = (obs, oldVal, newVal) -> updateSelectionStyle(newVal);
        stageEntry.getIsSelectedProperty().addListener(selectionListener);
        updateSelectionStyle(stageEntry.getIsSelectedProperty().get());

        // Bind duplicate warning state
        duplicateListener = (obs, oldVal, newVal) -> duplicateBadge.setVisible(newVal);
        stageEntry.getIsMaybeDuplicateProperty().addListener(duplicateListener);
        duplicateBadge.setVisible(stageEntry.getIsMaybeDuplicateProperty().get());

        // Thumbnail and image orientation
        applyThumbnail(stageEntry);
    }

    private void applyThumbnail(StageEntry stageEntry) {
        byte[] jpeg = stageEntry.thumbnail;
        if (jpeg != null && jpeg.length > 0) {
            Image image = new Image(new ByteArrayInputStream(jpeg),
                    450,
                    300,
                    true,
                    true);

            thumbnailImageView.setImage(image);
            thumbnailImageView.setRotate(0);

            thumbnailImageView.fitWidthProperty().unbind();
            thumbnailImageView.fitHeightProperty().unbind();

            if (parentGridView != null) {
                thumbnailImageView.fitWidthProperty().bind(parentGridView.cellWidthProperty().subtract(12));
                thumbnailImageView.fitHeightProperty().bind(parentGridView.cellHeightProperty().subtract(FOOTER_HEIGHT + 12));
            } else {
                thumbnailImageView.setFitWidth(240);
                thumbnailImageView.setFitHeight(160);
            }
        } else {
            thumbnailImageView.setImage(null);
        }
    }

    private void setupOptionsMenu() {
        optionsMenu = new ContextMenu();

        MenuItem menuItem = new MenuItem("Remove entry");
        menuItem.getStyleClass().add("menu-item-danger");
        menuItem.setOnAction(event -> {
            event.consume();
            if (removeAction != null && getItem() != null) {
                removeAction.accept(getItem());
            }
            optionsMenu.hide();
        });

        optionsMenu.getItems().add(menuItem);

        cardRoot.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                if (!optionsMenu.isShowing()) {
                    optionsMenu.show(cardRoot, event.getScreenX(), event.getScreenY());
                } else {
                    optionsMenu.hide();
                }
                event.consume();
            }
        });
    }

    private void updateSelectionStyle(boolean isSelected) {
        if (isSelected) {
            cardRoot.getStyleClass().remove("import-card-deselected");
            if (!cardRoot.getStyleClass().contains("import-card-selected")) {
                cardRoot.getStyleClass().add("import-card-selected");
            }
        } else {
            cardRoot.getStyleClass().remove("import-card-selected");
            if (!cardRoot.getStyleClass().contains("import-card-deselected")) {
                cardRoot.getStyleClass().add("import-card-deselected");
            }
        }
    }
}