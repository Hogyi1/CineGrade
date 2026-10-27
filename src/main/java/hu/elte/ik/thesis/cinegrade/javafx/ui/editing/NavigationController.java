package hu.elte.ik.thesis.cinegrade.javafx.ui.editing;

import hu.elte.ik.thesis.cinegrade.app.managers.user.UserManager;
import hu.elte.ik.thesis.cinegrade.domain.enums.Theme;
import hu.elte.ik.thesis.cinegrade.domain.enums.ViewType;
import hu.elte.ik.thesis.cinegrade.domain.enums.WorkspacePanel;
import hu.elte.ik.thesis.cinegrade.domain.navigation.SceneManager;
import hu.elte.ik.thesis.cinegrade.domain.theme.ThemeManager;
import hu.elte.ik.thesis.cinegrade.javafx.ui.animations.TransitionFactory;
import javafx.animation.ParallelTransition;
import javafx.animation.Transition;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;

public class NavigationController {

    private static final org.apache.logging.log4j.Logger logger = org.apache.logging.log4j.LogManager.getLogger(NavigationController.class);

    private final SceneManager sceneManager;
    private final UserManager userManager;
    private final ObjectProperty<WorkspacePanel> currentPanelProperty = new SimpleObjectProperty<>(null);
    private final ConcurrentLinkedDeque<ViewHistory> viewHistories = new ConcurrentLinkedDeque<>();

    @FXML
    private Label catalogTitleLabel;
    @FXML
    private Region brandSeparator;
    @FXML
    private StackPane navPillContainer;
    @FXML
    private Region selectionBubble;
    @FXML
    private HBox tabsContainer;
    @FXML
    private Label importTab;
    @FXML
    private Label developTab;
    @FXML
    private Label exportTab;
    @FXML
    private Button backToProjectsBtn;
    @FXML
    private Button userProfileBtn;
    @FXML
    private ToggleButton themeToggleBtn;

    private Transition bubbleTransition;
    private ChangeListener<Number> pendingTabWidthListener;
    private ThemeManager themeManager;
    private Label pendingTab;

    public NavigationController(SceneManager sceneManager, UserManager userManager, ThemeManager themeManager) {
        this.sceneManager = sceneManager;
        this.userManager = userManager;
        this.themeManager = themeManager;
        logger.debug("NavigationController instantiated");
    }

    @FXML
    private void initialize() {
        logger.debug("Initializing NavigationController UI bindings");
        selectionBubble.setManaged(false);

        importTab.setOnMouseClicked(e -> {
            logger.debug("Import tab clicked");
            switchTo(WorkspacePanel.IMPORT);
        });

        developTab.setOnMouseClicked(e -> {
            logger.debug("Development tab clicked");
            switchTo(WorkspacePanel.DEVELOPMENT);
        });

        exportTab.setOnMouseClicked(e -> {
            logger.debug("Export tab clicked");
            switchTo(WorkspacePanel.EXPORT);
        });

        backToProjectsBtn.setOnMouseClicked(e -> {
            logger.info("Back to projects button clicked, returning to PROJECT_MENU");
            sceneManager.switchView(ViewType.PROJECT_MENU);
        });

        themeToggleBtn.setOnAction(e -> {
            logger.debug("Theme toggle button clicked");
            themeManager.toggleTheme();
        });
        themeToggleBtn.setSelected(themeManager.getTheme() == Theme.DARK);
        themeManager.themeProperty().addListener((obs, oldTheme, newTheme) -> {
            themeToggleBtn.setSelected(newTheme == Theme.DARK);
        });

        // Keep bubble aligned when window or capsule resizes
        navPillContainer.widthProperty().addListener((obs, oldW, newW) -> {
            Label activeTab = getTabForPanel(currentPanelProperty.get());
            if (activeTab != null && activeTab.getWidth() > 0) {
                logger.trace("Nav pill container width resized from {} to {}, updating bubble position", oldW, newW);
                animateBubbleTo(activeTab, false);
            }
        });

        // Snap bubble to initial tab once layout pass completes
        Platform.runLater(() -> {
            Label activeTab = getTabForPanel(currentPanelProperty.get());
            if (activeTab != null) {
                logger.debug("Performing initial layout snap for active tab: {}", activeTab.getText());
                animateBubbleTo(activeTab, false);
                updateTabStyles(activeTab);
            }
        });
    }

    public void switchToPreviousPanel() {
        ViewHistory viewHistory = viewHistories.poll();
        if (viewHistory == null || viewHistory.previous == null || viewHistory.previous.current() == null) {
            logger.debug("No previous panel found in history, switching to fallback DEVELOPMENT panel");
            switchTo(WorkspacePanel.DEVELOPMENT);
            return;
        }

        logger.debug("Switching to previous panel from history: {}", viewHistory.previous.current());
        switchTo(viewHistory.previous.current());
    }

    public void setWorkspaceMode(boolean isWorkspace, String catalogTitle, WorkspacePanel startingPanel) {
        logger.info("Setting workspace mode: isWorkspace={}, catalogTitle='{}', startingPanel={}",
                isWorkspace, catalogTitle, startingPanel);
        if (!isWorkspace) {
            logger.debug("Disabling workspace mode and hiding navigation controls");
            navPillContainer.setVisible(false);
            navPillContainer.setManaged(false);
            catalogTitleLabel.setVisible(false);
            catalogTitleLabel.setManaged(false);
            brandSeparator.setVisible(false);
            brandSeparator.setManaged(false);
            backToProjectsBtn.setVisible(false);
            backToProjectsBtn.setManaged(false);
            return;
        }

        logger.debug("Enabling workspace mode and playing entrance transitions");
        navPillContainer.setManaged(true);
        catalogTitleLabel.setManaged(true);
        brandSeparator.setManaged(true);
        brandSeparator.setVisible(true);
        backToProjectsBtn.setManaged(true);

        ParallelTransition slideUp = TransitionFactory
                .createFadeSlideUp(navPillContainer, Duration.millis(500), 25, false, null);
        ParallelTransition slideRight = TransitionFactory
                .createFadeSlideRight(catalogTitleLabel, Duration.millis(500), 25, false, null);
        ParallelTransition slideLeft = TransitionFactory
                .createFadeSlideLeft(backToProjectsBtn, Duration.millis(500), 25, false, null);
        slideUp.play();
        slideRight.play();
        slideLeft.play();

        if (catalogTitle != null && !catalogTitle.isBlank()) {
            catalogTitleLabel.setText(catalogTitle);
        }
        WorkspacePanel initialPanel = (startingPanel != null) ? startingPanel : WorkspacePanel.IMPORT;
        switchTo(initialPanel);
    }

    public ReadOnlyObjectProperty<WorkspacePanel> getNavigationProperty() {
        return currentPanelProperty;
    }

    public void switchTo(WorkspacePanel nextPanel) {
        if (nextPanel == null) {
            logger.warn("Attempted to switch to null workspace panel");
            return;
        }

        Label targetTab = getTabForPanel(nextPanel);

        if (nextPanel == currentPanelProperty.get()) {
            logger.debug("Already on panel {}, skipping panel transition", nextPanel);
            if (selectionBubble.getWidth() <= 0 && targetTab != null) {
                animateBubbleTo(targetTab, false);
            }
            return;
        }

        logger.info("Switching workspace panel from {} to {}", currentPanelProperty.get(), nextPanel);
        ViewHistory previousHistory = viewHistories.peek();
        viewHistories.push(new ViewHistory(previousHistory, currentPanelProperty.get(), nextPanel));
        currentPanelProperty.set(nextPanel);

        if (targetTab != null) {
            updateTabStyles(targetTab);
            animateBubbleTo(targetTab, true);
        }
    }

    // [AI-GENERATED METHOD]
    private void animateBubbleTo(Label targetTab, boolean animated) {
        if (targetTab == null || tabsContainer == null || navPillContainer == null) {
            logger.trace("Skipping animateBubbleTo: targetTab={}, tabsContainer={}, navPillContainer={}",
                    targetTab, tabsContainer, navPillContainer);
            return;
        }

        if (pendingTabWidthListener != null && pendingTab != null) {
            logger.trace("Cleaning up previous pendingTabWidthListener");
            pendingTab.widthProperty().removeListener(pendingTabWidthListener);
            pendingTabWidthListener = null;
            pendingTab = null;
        }

        double targetWidth = targetTab.getWidth();
        if (targetWidth <= 0) {
            logger.trace("Target tab {} not yet rendered (width={}), registering listener",
                    targetTab.getText(), targetWidth);
            pendingTab = targetTab;
            pendingTabWidthListener = (obs, oldVal, newVal) -> {
                if (newVal.doubleValue() > 0) {
                    targetTab.widthProperty().removeListener(pendingTabWidthListener);
                    pendingTabWidthListener = null;
                    pendingTab = null;
                    Platform.runLater(() -> animateBubbleTo(targetTab, false));
                }
            };
            targetTab.widthProperty().addListener(pendingTabWidthListener);
            return;
        }

        Bounds pillBounds = null;
        if (targetTab.getScene() != null && navPillContainer.getScene() != null) {
            Bounds sceneBounds = targetTab.localToScene(targetTab.getBoundsInLocal());
            if (sceneBounds != null) {
                pillBounds = navPillContainer.sceneToLocal(sceneBounds);
            }
        }

        double targetX;
        double targetY;
        double targetHeight = 32.0;

        if (pillBounds != null) {
            targetX = pillBounds.getMinX();
            targetY = pillBounds.getMinY();
            targetWidth = pillBounds.getWidth();
            if (pillBounds.getHeight() > 0) {
                targetHeight = pillBounds.getHeight();
            }
        } else {
            targetX = tabsContainer.getBoundsInParent().getMinX() + targetTab.getBoundsInParent().getMinX();
            targetY = (navPillContainer.getHeight() - targetHeight) / 2.0;
        }
        if (targetY <= 0) {
            targetY = 3.0;
        }

        if (bubbleTransition != null) {
            bubbleTransition.stop();
        }

        final double currentBubbleWidth = selectionBubble.getWidth();
        if (!animated || currentBubbleWidth <= 0) {
            logger.trace("Relocating bubble instantly to x={}, y={}, w={}, h={}", targetX, targetY, targetWidth, targetHeight);
            selectionBubble.resizeRelocate(targetX, targetY, targetWidth, targetHeight);
            return;
        }

        final double startX = selectionBubble.getLayoutX();
        final double startW = currentBubbleWidth;
        final double diffX = targetX - startX;
        final double diffW = targetWidth - startW;
        final double finalTargetX = targetX;
        final double finalTargetWidth = targetWidth;
        final double finalTargetY = targetY;
        final double finalTargetHeight = targetHeight;

        logger.trace("Animating bubble from x={}, w={} to x={}, w={}", startX, startW, finalTargetX, finalTargetWidth);
        bubbleTransition = new javafx.animation.Transition() {
            {
                setCycleDuration(Duration.millis(250));
                setInterpolator(javafx.animation.Interpolator.EASE_BOTH);
            }

            @Override
            protected void interpolate(double frac) {
                double x = startX + diffX * frac;
                double w = startW + diffW * frac;
                selectionBubble.resizeRelocate(x, finalTargetY, w, finalTargetHeight);
            }
        };
        bubbleTransition.setOnFinished(e -> {
            Platform.runLater(() -> {
                if (targetTab.getScene() != null && navPillContainer.getScene() != null) {
                    javafx.geometry.Bounds finalBounds = navPillContainer.sceneToLocal(targetTab.localToScene(targetTab.getBoundsInLocal()));
                    if (finalBounds != null) {
                        selectionBubble.resizeRelocate(finalBounds.getMinX(), finalBounds.getMinY(), finalBounds.getWidth(), finalBounds.getHeight());
                        return;
                    }
                }
                selectionBubble.resizeRelocate(finalTargetX, finalTargetY, finalTargetWidth, finalTargetHeight);
            });
        });
        bubbleTransition.play();
    }

    // [AI-GENERATED METHOD]
    private Label getTabForPanel(WorkspacePanel panel) {
        if (panel == null) {
            return importTab;
        }
        return switch (panel) {
            case IMPORT -> importTab;
            case DEVELOPMENT -> developTab;
            case EXPORT -> exportTab;
        };
    }

    // [AI-GENERATED METHOD]
    private void updateTabStyles(Label activeLabel) {
        logger.trace("Updating tab styles for active tab: {}", activeLabel != null ? activeLabel.getText() : "none");
        Label[] tabs = {importTab, developTab, exportTab};
        for (Label tab : tabs) {
            if (tab == activeLabel) {
                tab.getStyleClass().remove("nav-item-inactive");
                if (!tab.getStyleClass().contains("nav-item-active")) {
                    tab.getStyleClass().add("nav-item-active");
                }
            } else {
                tab.getStyleClass().remove("nav-item-active");
                if (!tab.getStyleClass().contains("nav-item-inactive")) {
                    tab.getStyleClass().add("nav-item-inactive");
                }
            }
        }
    }

    private record ViewHistory(ViewHistory previous, WorkspacePanel current, WorkspacePanel next) {
    }
}
