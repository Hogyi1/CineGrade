package hu.elte.ik.thesis.cinegrade.javafx.ui.catalog;

import hu.elte.ik.thesis.cinegrade.app.managers.catalog.CatalogManager;
import hu.elte.ik.thesis.cinegrade.app.managers.user.UserManager;
import hu.elte.ik.thesis.cinegrade.domain.catalog.Catalog;
import hu.elte.ik.thesis.cinegrade.domain.navigation.NavigationManager;
import javafx.beans.InvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.*;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Comparator;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class MainMenuController {

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

    @FXML
    private ImageView sortNameIcon;
    @FXML
    private ImageView sortSizeIcon;
    @FXML
    private ImageView sortRecentIcon;
    @FXML
    private ImageView sortCreationIcon;

    private final CatalogManager catalogManager;
    private final UserManager userManager;
    private final NavigationManager navigationManager;
    private ObservableList<Catalog> catalogObservableList;
    private FilteredList<Catalog> filteredList;
    private SortedList<Catalog> sortedList;
    private final ObjectProperty<Catalog> selectedProperty = new SimpleObjectProperty<>();

    private final Consumer<Catalog> removeRecentConsumer;
    private final Consumer<Catalog> deleteConsumer;

    public MainMenuController(CatalogManager catalogManager, UserManager userManager, NavigationManager navigationManager) {
        this.catalogManager = catalogManager;
        this.userManager = userManager;
        this.navigationManager = navigationManager;

        deleteConsumer = catalogManager::deleteCatalog;
        removeRecentConsumer = catalogManager::removeFromRecent;
    }

    @FXML
    public void initialize() {
        catalogObservableList = catalogManager.getCatalogs();

        filteredList = new FilteredList<>(catalogObservableList, pr -> true);
        sortedList = new SortedList<>(filteredList, Comparator.comparing(Catalog::getCatalogName));

        catalogListView.setCellFactory(lvC -> {
            CatalogViewItem viewItem = new CatalogViewItem(removeRecentConsumer, deleteConsumer);
            return viewItem;
        });

        catalogListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != selectedProperty.getValue() && newValue != null) {
                selectedProperty.set(newValue);
                catalogManager.openCatalog(newValue);
            }
        });

        searchField.setOnAction(e -> {
            String text = searchField.getText().trim().toLowerCase();
            filteredList.setPredicate(catalog -> text.isEmpty() || catalog.getCatalogName().contains(text));
        });

        setupWelcomeLabel();
        setupButtons();
        catalogListView.setItems(sortedList);
    }

    private void setupButtons() {
        newProjectButton.setOnMouseClicked(e -> catalogManager.createNewProject());

        openProjectButton.setOnMouseClicked(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Open CineGrade project");
            fileChooser.getExtensionFilters().addAll(new ExtensionFilter("CG projects", "*.cgproj"));
            Window mainWindow = navigationManager.getActiveWindow();
            File selectedFile = fileChooser.showOpenDialog(mainWindow);
            if (selectedFile != null) {
                catalogManager.openCatalog(selectedFile);
            }
        });

        homeNav.setOnMouseClicked(e -> {
            // Handle home navigation click
        });

        socialNav.setOnMouseClicked(e -> {
            // Handle social navigation click
        });
    }

    private void setupWelcomeLabel() {
        String userName = userManager.getUsername();
        String text = userName.isBlank() ? "Welcome bacl!" : "Welcome, " + userName + "!";
        welcomeLabel.setText(text);
    }
}
