package hu.elte.ik.thesis.cinegrade.javafx.ui.components;

import hu.elte.ik.thesis.cinegrade.domain.editing.imports.StageEntry;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Controller for the staged photo card item in the media import panel.
 */
public class ImportPhotoCard extends VBox{

    @FXML
    private VBox cardRoot;

    @FXML
    private StackPane thumbnailContainer;

    @FXML
    private ImageView thumbnailImageView;

    @FXML
    private CheckBox selectCheckBox;

    @FXML
    private Button removeButton;

    @FXML
    private Label fileNameLabel;

    @FXML
    private Label fileSizeLabel;

    @FXML
    private Label metaLabel;

    @FXML
    private Label formatBadge;

    private StageEntry stageEntry;
    private Runnable removeAction;

    @FXML
    public void initialize() {

    }

    /**
     * Dynamically binds the card dimensions to the thumbnail slider value.
     */
    public void bindSize(DoubleProperty sizeProperty) {
        if (sizeProperty == null) {
            return;
        }

        cardRoot.prefWidthProperty().bind(sizeProperty);
        cardRoot.maxWidthProperty().bind(sizeProperty);
        thumbnailImageView.fitWidthProperty().bind(sizeProperty);

        // Maintain approximately 3:2 aspect ratio for thumbnail container
        thumbnailContainer.prefHeightProperty().bind(sizeProperty.multiply(0.65));
        thumbnailContainer.maxHeightProperty().bind(sizeProperty.multiply(0.65));
        thumbnailImageView.fitHeightProperty().bind(sizeProperty.multiply(0.65));
    }
}
