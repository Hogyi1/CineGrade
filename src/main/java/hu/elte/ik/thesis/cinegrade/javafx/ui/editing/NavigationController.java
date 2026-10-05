package hu.elte.ik.thesis.cinegrade.javafx.ui.editing;

import hu.elte.ik.thesis.cinegrade.app.managers.user.UserManager;
import hu.elte.ik.thesis.cinegrade.domain.context.CatalogContext;
import hu.elte.ik.thesis.cinegrade.domain.enums.ErrorCode;
import hu.elte.ik.thesis.cinegrade.domain.enums.ViewType;
import hu.elte.ik.thesis.cinegrade.domain.enums.WorkspacePanel;
import hu.elte.ik.thesis.cinegrade.domain.exceptions.CineGradeException;
import hu.elte.ik.thesis.cinegrade.domain.navigation.SceneManager;
import hu.elte.ik.thesis.cinegrade.javafx.ui.animations.TransitionFactory;
import javafx.animation.ParallelTransition;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.util.concurrent.ConcurrentLinkedQueue;

public class NavigationController {

    private final SceneManager sceneManager;
    private final UserManager userManager;
    private final ObjectProperty<WorkspacePanel> currentPanelProperty = new SimpleObjectProperty<>(null);
    private final ConcurrentLinkedQueue<ViewHistory> viewHistories = new ConcurrentLinkedQueue<>();

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

    private javafx.animation.Transition bubbleTransition;
    private javafx.beans.value.ChangeListener<Number> pendingTabWidthListener;
    private Label pendingTab;

    public NavigationController(SceneManager sceneManager, UserManager userManager) {
        this.sceneManager = sceneManager;
        this.userManager = userManager;
    }

    @FXML
    private void initialize() {
        selectionBubble.setManaged(false);

        importTab.setOnMouseClicked(e -> {
            switchTo(WorkspacePanel.IMPORT);
        });

        developTab.setOnMouseClicked(e -> {
            switchTo(WorkspacePanel.DEVELOPMENT);
        });

        exportTab.setOnMouseClicked(e -> {
            switchTo(WorkspacePanel.EXPORT);
        });

        backToProjectsBtn.setOnMouseClicked(e -> {
            sceneManager.switchView(ViewType.PROJECT_MENU);
        });

        // Keep bubble aligned when window or capsule resizes
        navPillContainer.widthProperty().addListener((obs, oldW, newW) -> {
            Label activeTab = getTabForPanel(currentPanelProperty.get());
            if (activeTab != null && activeTab.getWidth() > 0) {
                animateBubbleTo(activeTab, false);
            }
        });

        // Snap bubble to initial tab once layout pass completes
        Platform.runLater(() -> {
            Label activeTab = getTabForPanel(currentPanelProperty.get());
            if (activeTab != null) {
                animateBubbleTo(activeTab, false);
                updateTabStyles(activeTab);
            }
        });
    }

    // [AI-GENERATED METHOD]
    public void setWorkspaceMode(boolean isWorkspace, String catalogTitle, WorkspacePanel startingPanel) {
        if (!isWorkspace) {
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

        navPillContainer.setManaged(true);
        catalogTitleLabel.setManaged(true);
        brandSeparator.setManaged(true);
        brandSeparator.setVisible(true);
        backToProjectsBtn.setManaged(true);

        ParallelTransition slideUp = TransitionFactory
                .createFadeSlideUp(navPillContainer, Duration.millis(1000), 50, false, null);
        ParallelTransition slideRight = TransitionFactory
                .createFadeSlideRight(catalogTitleLabel, Duration.millis(1000), 50, false, null);
        ParallelTransition slideLeft = TransitionFactory
                .createFadeSlideLeft(backToProjectsBtn, Duration.millis(1000), 50, false, null);
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

    private void switchTo(WorkspacePanel nextPanel) {
        if (nextPanel == null) {
            return;
        }

        Label targetTab = getTabForPanel(nextPanel);

        if (nextPanel == currentPanelProperty.get()) {
            if (selectionBubble.getWidth() <= 0 && targetTab != null) {
                animateBubbleTo(targetTab, false);
            }
            return;
        }

        ViewHistory previousHistory = viewHistories.peek();
        viewHistories.offer(new ViewHistory(previousHistory, currentPanelProperty.get(), nextPanel));
        currentPanelProperty.set(nextPanel);

        if (targetTab != null) {
            updateTabStyles(targetTab);
            animateBubbleTo(targetTab, true);
        }
    }

    // [AI-GENERATED METHOD]
    private void animateBubbleTo(Label targetTab, boolean animated) {
        if (targetTab == null || tabsContainer == null || navPillContainer == null) {
            return;
        }

        if (pendingTabWidthListener != null && pendingTab != null) {
            pendingTab.widthProperty().removeListener(pendingTabWidthListener);
            pendingTabWidthListener = null;
            pendingTab = null;
        }

        double targetWidth = targetTab.getWidth();
        if (targetWidth <= 0) {
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

        javafx.geometry.Bounds pillBounds = null;
        if (targetTab.getScene() != null && navPillContainer.getScene() != null) {
            javafx.geometry.Bounds sceneBounds = targetTab.localToScene(targetTab.getBoundsInLocal());
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
