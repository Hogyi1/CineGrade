package hu.elte.ik.thesis.cinegrade.javafx.ui.editing;

import hu.elte.ik.thesis.cinegrade.domain.context.CatalogContext;
import hu.elte.ik.thesis.cinegrade.domain.enums.ErrorCode;
import hu.elte.ik.thesis.cinegrade.domain.enums.WorkspacePanel;
import hu.elte.ik.thesis.cinegrade.domain.exceptions.CineGradeException;
import hu.elte.ik.thesis.cinegrade.domain.navigation.ControllerFactory;
import hu.elte.ik.thesis.cinegrade.domain.navigation.SceneManager;
import hu.elte.ik.thesis.cinegrade.javafx.ui.PanelController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.EnumMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Coordinates navigation between the panels shown in a catalog workspace.
 * Loaded panels are cached so their controllers and UI state can be reused.
 */
public class WorkspaceController {

    private static final Logger logger = LogManager.getLogger(WorkspaceController.class);
    private final CatalogContext context;
    private final ControllerFactory controllerFactory;
    private final ConcurrentHashMap<WorkspacePanel, CachedPanel> cachedPanels = new ConcurrentHashMap<>();
    @FXML
    private StackPane workspacePanelContainer;
    @FXML
    private BorderPane editPage;
    @FXML
    private NavigationController navigationBarController;
    private CachedPanel currentPanel;

    /**
     * Creates a workspace controller with the catalog context and controller factory
     * required to load its panels.
     *
     * @param context the catalog and initial-panel context for this workspace
     * @param controllerFactory the factory used to construct FXML controllers
     */
    public WorkspaceController(CatalogContext context, ControllerFactory controllerFactory) {
        this.controllerFactory = controllerFactory;
        this.context = context;
    }

    /**
     * Initializes the navigation bar and listens for workspace panel selections.
     */
    @FXML
    private void initialize() {
        logger.info("Initializing workspace for catalog '{}' with starting panel {}", context.catalog.getCatalogName(), context.startingPanel);
        navigationBarController
                .getNavigationProperty()
                .addListener((obs, oldValue, newValue) -> {
                    logger.info("Navigation changed from {} to {}", oldValue, newValue);
                    changeWorkspacePanel(newValue);
                });
        navigationBarController.setWorkspaceMode(true, context.catalog.getCatalogName(), context.startingPanel);
    }

    /**
     * Displays the requested panel, loading and caching it on its first visit.
     * The current panel is allowed to exit before the next panel is entered.
     *
     * @param panel the workspace panel to display
     */
    public void changeWorkspacePanel(WorkspacePanel panel) {
        if (panel == null) {
            logger.warn("Workspace panel change requested with null panel");
            return;
        }

        CachedPanel cachedPanel = cachedPanels.computeIfAbsent(panel, this::loadPanel);
        if (cachedPanel == null) {
            logger.error("Unable to switch to workspace panel {} because it could not be loaded", panel);
            return;
        }

        String previousPanel = currentPanel != null && currentPanel.controller != null
                ? currentPanel.controller.getClass().getSimpleName()
                : "none";
        logger.info("Switching workspace panel from {} to {}", previousPanel, panel);

        if (currentPanel != null && currentPanel.controller != null && currentPanel.controller.canExit()) {
            logger.debug("Exiting current workspace panel before switching to {}", panel);
            currentPanel.controller.onExit();
        }

        cachedPanel.controller.onEnter();
        if (currentPanel != null){
            workspacePanelContainer.getChildren().remove(currentPanel.root);
        }
        workspacePanelContainer.getChildren().add(cachedPanel.root);
        StackPane.setAlignment(cachedPanel.root, Pos.CENTER);
        logger.debug("Workspace panel {} is now active", panel);
        currentPanel = cachedPanel;
    }

    /**
     * Loads a panel's FXML and obtains its controller.
     *
     * @param panel the panel whose FXML should be loaded
     * @return the loaded panel root and its controller
     * @throws CineGradeException if the panel's FXML cannot be loaded
     */
    private CachedPanel loadPanel(WorkspacePanel panel) {
        logger.debug("Loading workspace panel {}", panel);
        FXMLLoader loader = createLoader(panel.getFxmlPath());
        try {
            Parent root = loader.load();
            PanelController controller = loader.getController();
            logger.info("Workspace panel {} loaded successfully", panel);
            return new CachedPanel(controller, root);
        } catch (Exception e) {
            throw new CineGradeException(ErrorCode.UNKNOWN_ERROR, "Failed to load workspace panel: " + panel, e);
        }
    }

    /**
     * Creates an FXML loader configured to use the workspace's controller factory.
     *
     * @param fxmlPath the resource path of the FXML document
     * @return the configured loader
     */
    private FXMLLoader createLoader(String fxmlPath) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        loader.setControllerFactory(controllerFactory);
        return loader;
    }

    /**
     * Holds the UI root and controller for a loaded workspace panel.
     *
     * @param controller the controller responsible for panel lifecycle behavior
     * @param root the panel's FXML root node
     */
    private record CachedPanel(PanelController controller, Parent root) {
    }
}
