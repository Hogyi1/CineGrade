package hu.elte.ik.thesis.cinegrade.javafx.ui.catalog;

import hu.elte.ik.thesis.cinegrade.app.managers.catalog.CatalogManager;
import hu.elte.ik.thesis.cinegrade.app.managers.user.UserManager;
import hu.elte.ik.thesis.cinegrade.domain.catalog.Catalog;
import hu.elte.ik.thesis.cinegrade.domain.enums.ErrorCode;
import hu.elte.ik.thesis.cinegrade.domain.enums.ViewType;
import hu.elte.ik.thesis.cinegrade.domain.error.ErrorHandler;
import hu.elte.ik.thesis.cinegrade.domain.exceptions.CineGradeException;
import hu.elte.ik.thesis.cinegrade.domain.navigation.NavigationManager;
import hu.elte.ik.thesis.cinegrade.domain.theme.ThemeManager;
import hu.elte.ik.thesis.cinegrade.javafx.ui.dialog.NewProjectDialogController;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.function.Consumer;

public class MainMenuController {

    private static final Logger logger = LogManager.getLogger(MainMenuController.class);
    private final CatalogManager catalogManager;
    private final UserManager userManager;
    private final NavigationManager navigationManager;
    private final ObjectProperty<Catalog> selectedProperty = new SimpleObjectProperty<>();
    private final Consumer<Catalog> removeRecentConsumer;
    private final Consumer<Catalog> deleteConsumer;
    private final Stage modalStage;

    @FXML
    private Button newProjectButton;
    @FXML
    private Button openProjectButton;
    @FXML
    private HBox homeNav;
    @FXML
    private HBox socialNav;
    @FXML
    private Label welcomeLabel;
    @FXML
    private TextField searchField;
    @FXML
    private ListView<Catalog> catalogListView;
    @FXML
    private HBox sizeFilter;
    @FXML
    private HBox nameFilter;
    @FXML
    private HBox recentFilter;
    @FXML
    private HBox creationFilter;
    private HBox activeFilter;
    @FXML
    private ImageView sortNameIcon;
    @FXML
    private ImageView sortSizeIcon;
    @FXML
    private ImageView sortRecentIcon;
    @FXML
    private ImageView sortCreationIcon;
    private ObservableList<Catalog> catalogObservableList;
    private FilteredList<Catalog> filteredList;
    private SortedList<Catalog> sortedList;

    public MainMenuController(CatalogManager catalogManager, UserManager userManager, NavigationManager navigationManager) {
        this.catalogManager = catalogManager;
        this.userManager = userManager;
        this.navigationManager = navigationManager;
        this.modalStage = new Stage();

        deleteConsumer = catalogManager::deleteCatalog;
        removeRecentConsumer = catalogManager::removeFromRecent;
    }

    @FXML
    public void initialize() {
        catalogObservableList = catalogManager.getCatalogs();
        filteredList = new FilteredList<>(catalogObservableList, pr -> true);
        sortedList = new SortedList<>(filteredList, Comparator.comparing(Catalog::getCatalogName));

        setupListView();
        setupWelcomeLabel();
        setupButtons();
        catalogListView.setItems(sortedList);
    }

    private void setupButtons() {
        newProjectButton.setOnMouseClicked(e -> createNewProjectModal());

        openProjectButton.setOnMouseClicked(e -> {
            logger.info("Open Project button clicked, opening FileChooser");
            FileChooser fileChooser = new FileChooser();
            fileChooser.setInitialDirectory(catalogManager.getDefaultPath().toFile());
            fileChooser.setTitle("Open CineGrade project");
            fileChooser.getExtensionFilters().addAll(new ExtensionFilter("CG projects", "*.cgproj"));
            Window mainWindow = navigationManager.getActiveWindow();
            File selectedFile = fileChooser.showOpenDialog(mainWindow);
            if (selectedFile != null) {
                logger.info("Selected project file from chooser: {}", selectedFile.getAbsolutePath());
                try {
                    catalogManager.openCatalog(selectedFile);
                    logger.info("Catalog opened successfully from file {} ", selectedFile);
                    // navigationManager.switchView(ViewType.TEST_MENU);
                    ErrorHandler.getInstance().handle(new CineGradeException(ErrorCode.UNKNOWN_ERROR, "Catalog selected, database connection opened"));
                    e.consume();
                } catch (Exception ex) {
                    ErrorHandler.getInstance().handle(ex);
                }
            }
        });

        homeNav.setOnMouseClicked(e -> {
            //TODO
            // Handle home navigation click
        });

        socialNav.setOnMouseClicked(e -> {
            //TODO
            // Handle social navigation click
            navigationManager.switchView(ViewType.TEST_MENU);
        });

        //socialNav.setDisable(true);
    }

    private void setupWelcomeLabel() {
        String userName = userManager.getUsername();
        String text = userName.isBlank() ? "Welcome back!" : "Welcome back, " + userName + "!";
        welcomeLabel.setText(text);
    }

    private void setupListView() {
        catalogListView.setCellFactory(lvC -> {
            CatalogViewItem viewItem = new CatalogViewItem(removeRecentConsumer, deleteConsumer);
            return viewItem;
        });

        catalogListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != selectedProperty.getValue() && newValue != null) {
                selectedProperty.set(null);
                logger.info("Catalog selected from recent list: {} (path: {})", newValue.getCatalogName(), newValue.getCatalogPath());
                try {
                    catalogManager.openCatalog(newValue);
                    logger.info("Catalog opened successfully, switching view to {}", ViewType.EDIT_PAGE);
                    // navigationManager.switchView(ViewType.TEST_MENU);
                    ErrorHandler.getInstance().handle(new CineGradeException(ErrorCode.UNKNOWN_ERROR, "Catalog selected, database connection opened"));
                } catch (Exception ex) {
                    ErrorHandler.getInstance().handle(ex);
                    Platform.runLater(() ->{
                        selectedProperty.set(null);
                        catalogListView.getSelectionModel().clearSelection();
                    });
                }
            }
        });

        searchField.textProperty().addListener((obs, oldValue, newValue) -> {
            String text = searchField.getText().trim().toLowerCase();
            filteredList.setPredicate(catalog -> text.isEmpty() || catalog.getCatalogName().toLowerCase().contains(text));
        });

        Comparator<Catalog> nameComparator = Comparator.comparing(Catalog::getCatalogName, String.CASE_INSENSITIVE_ORDER);
        Comparator<Catalog> sizeComparator = Comparator.comparing(Catalog::getSize);
        Comparator<Catalog> recentComparator = Comparator.comparing(Catalog::getLastOpenedAt);
        Comparator<Catalog> createdComparator = Comparator.comparing(Catalog::getCreatedAt);

        setupSortFilter(nameFilter, sortNameIcon, nameComparator);
        setupSortFilter(sizeFilter, sortSizeIcon, sizeComparator);
        setupSortFilter(recentFilter, sortRecentIcon, recentComparator);
        setupSortFilter(creationFilter, sortCreationIcon, createdComparator);
    }

    private void setupSortFilter(HBox filterBox, ImageView sortIcon, Comparator<Catalog> baseComparator) {
        filterBox.getProperties().put("active", false);
        filterBox.getProperties().put("reversed", false);
        sortIcon.setVisible(false);

        filterBox.setOnMouseClicked(e -> {
            boolean isActive = (boolean) filterBox.getProperties().get("active");
            boolean isReversed = (boolean) filterBox.getProperties().get("reversed");

            if (activeFilter != null && activeFilter != filterBox) {
                activeFilter.getProperties().put("active", false);
                activeFilter.getProperties().put("reversed", false);
                hideAllSortIcons();
            }

            activeFilter = filterBox;

            if (isActive) {
                isReversed = !isReversed;
            } else {
                isActive = true;
                isReversed = false;
            }

            filterBox.getProperties().put("active", isActive);
            filterBox.getProperties().put("reversed", isReversed);

            sortIcon.setVisible(true);
            sortIcon.setRotate(isReversed ? 180 : 0);

            sortedList.setComparator(isReversed ? baseComparator.reversed() : baseComparator);
        });
    }

    private void hideAllSortIcons() {
        sortNameIcon.setVisible(false);
        sortSizeIcon.setVisible(false);
        sortRecentIcon.setVisible(false);
        sortCreationIcon.setVisible(false);
    }

    private void createNewProjectModal() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxmls/newProjectModal.fxml"));
            loader.setControllerFactory(factory -> new NewProjectDialogController(catalogManager::createNewProject, catalogManager.getDefaultPath(), navigationManager));

            Parent root = loader.load();
            NewProjectDialogController controller = loader.getController();

            root.getStyleClass().add(ThemeManager.get().getStyleClass());

            modalStage.initModality(Modality.APPLICATION_MODAL);
            modalStage.setTitle("CineGrade — New Project");
            modalStage.setResizable(false);
            modalStage.setScene(new Scene(root));
            modalStage.centerOnScreen();

            modalStage.showAndWait();
        } catch (IOException ex) {
            throw new CineGradeException(ErrorCode.REQ_RESOURCE_MISSING, ex, "/fxmls/newProjectModal.fxml");
        }
    }
}
