package hu.elte.ik.thesis.cinegrade.javafx.ui.editing;

import hu.elte.ik.thesis.cinegrade.domain.context.CatalogContext;
import hu.elte.ik.thesis.cinegrade.javafx.ui.PanelController;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.layout.FlowPane;

public class ImportPanelController implements PanelController {

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
    private ScrollPane stagedScrollPane;
    @FXML
    private FlowPane cardsFlowPane;
    @FXML
    private Button browseFilesButton;
    @FXML
    private Button cancelButton;
    @FXML
    private Button importButton;

    @Override
    public boolean canExit() {
        return false;
    }
    @Override
    public void onExit() {

    }
    @Override
    public void onEnter() {

    }
}
