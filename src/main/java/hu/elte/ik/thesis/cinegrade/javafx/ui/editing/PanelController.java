package hu.elte.ik.thesis.cinegrade.javafx.ui.editing;

public interface PanelController {
    public void setNavigationController(NavigationController navigationController);
    public boolean canExit();
    public void onExit();
    public void onEnter();
}
