package hu.elte.ik.thesis.cinegrade.javafx.ui.components;

import hu.elte.ik.thesis.cinegrade.domain.editing.imports.StageEntry;
import hu.elte.ik.thesis.cinegrade.infra.utils.FormatUtils;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.DoubleProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Controller for the staged photo card item in the media import panel.
 */
public class ImportPhotoCard {

    private final static Logger logger = LogManager.getLogger(ImportPhotoCard.class);
    private final int BASE_CARD_WIDTH = 285;
    private final double THUMBNAIL_ASPECT_RATIO = 3.0 / 2.0;

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

    private final StageEntry stageEntry;
    private final Consumer<StageEntry> removeAction;
    private ContextMenu removeMenu;

    public ImportPhotoCard(StageEntry stageEntry, Consumer<StageEntry> removeAction) {
        this.stageEntry = stageEntry;
        this.removeAction = removeAction;
    }

    @FXML
    private void initialize() throws MalformedURLException {
        fileSizeLabel.setText(FormatUtils.formatBytes(stageEntry.size));
        fileNameLabel.setText(stageEntry.getFile().getName());
        String fileType = stageEntry.isRaw ? stageEntry.jpegPath != null ? "[RAW + JPEG]" : "RAW" : stageEntry.fileType;
        metaLabel.setText("%s • %s".formatted(stageEntry.imageSize, fileType));
        formatBadge.setText(stageEntry.fileType);

        selectCheckBox.selectedProperty().bindBidirectional(stageEntry.getIsSelectedProperty());
        selectCheckBox.setDisable(true);
        selectCheckBox.setStyle("-fx-opacity: 1");

        cardRoot.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                e.consume();
                selectCheckBox.setSelected(!selectCheckBox.isSelected());
            }
        });

        stageEntry.getIsSelectedProperty().addListener((obs, oldVal, newVal) -> updateSelectionStyle(newVal));
        updateSelectionStyle(stageEntry.getIsSelectedProperty().get());

        Path thumbPath = stageEntry.thumbnailPath;
        if (thumbPath != null && Files.exists(thumbPath)) {
            thumbnailImageView.setImage(new Image(
                    thumbPath.toUri().toString(),
                    450, 300,
                    true, true, true
            ));
            thumbnailImageView.setRotate(stageEntry.rotation);
        }

        // [AI-GENERATED] Dynamic rounded clip ensuring thumbnails never bleed outside container
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
        clip.widthProperty().bind(thumbnailContainer.widthProperty());
        clip.heightProperty().bind(thumbnailContainer.heightProperty());
        clip.setArcWidth(12.0);
        clip.setArcHeight(12.0);
        thumbnailContainer.setClip(clip);

        setupOptionsMenu();

        stageEntry.getIsMaybeDuplicateProperty().addListener((obs, oldVal, newVal) -> {
            if (oldVal != newVal) {
                duplicateBadge.setVisible(newVal);
            }
        });
        duplicateBadge.setVisible(stageEntry.getIsMaybeDuplicateProperty().get());
    }

    private void setupOptionsMenu() {
        removeMenu = new ContextMenu();

        MenuItem menuItem = new MenuItem("Remove entry");
        menuItem.getStyleClass().add("menu-item-danger");
        menuItem.setOnAction(event -> {
            event.consume();
            removeAction.accept(stageEntry);
            removeMenu.hide();
        });

        removeMenu.getItems().add(menuItem);

        cardRoot.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                if (!removeMenu.isShowing()) {
                    removeMenu.show(cardRoot, event.getScreenX(), event.getScreenY());
                } else {
                    removeMenu.hide();
                }
                event.consume();
            }
        });
    }

    // [AI-GENERATED METHOD]
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

    /**
     * Dynamically binds the card dimensions to the thumbnail slider value.
     */
    // [AI-GENERATED]
    public void bindSize(DoubleProperty sizeProperty) {
        if (sizeProperty == null) {
            return;
        }

        // 1. Calculate dynamic card width: 285.0 * [0.5 ... 1.5] -> [142.5px ... 427.5px]
        DoubleBinding cardWidth = sizeProperty.multiply(BASE_CARD_WIDTH);
        DoubleBinding innerWidth = cardWidth.subtract(4.0); // Account for 2px card padding on left and right

        // 2. Calculate dynamic thumbnail height for 3:2 ratio (height = width / 1.5): [95px ... 285px]
        DoubleBinding thumbHeight = innerWidth.divide(THUMBNAIL_ASPECT_RATIO);

        // Bind the card root width (FlowPane queries prefWidth to arrange columns)
        if (cardRoot != null) {
            cardRoot.prefWidthProperty().bind(cardWidth);
            cardRoot.minWidthProperty().bind(cardWidth);
            cardRoot.maxWidthProperty().bind(cardWidth);
        }

        // Bind thumbnail container height
        if (thumbnailContainer != null) {
            thumbnailContainer.prefHeightProperty().bind(thumbHeight);
            thumbnailContainer.minHeightProperty().bind(thumbHeight);
            thumbnailContainer.maxHeightProperty().bind(thumbHeight);
        }

        // Bind ImageView bounds so the thumbnail scales smoothly with the card and stays inside bounds
        if (thumbnailImageView != null) {
            boolean isRotated = stageEntry.rotation == 90 || stageEntry.rotation == 270;
            if (isRotated) {
                thumbnailImageView.fitWidthProperty().bind(thumbHeight);
                thumbnailImageView.fitHeightProperty().bind(innerWidth);
            } else {
                thumbnailImageView.fitWidthProperty().bind(innerWidth);
                thumbnailImageView.fitHeightProperty().bind(thumbHeight);
            }
        }
    }
}
