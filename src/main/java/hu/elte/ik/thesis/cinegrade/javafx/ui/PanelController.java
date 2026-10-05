package hu.elte.ik.thesis.cinegrade.javafx.ui;

import hu.elte.ik.thesis.cinegrade.domain.context.CatalogContext;
import hu.elte.ik.thesis.cinegrade.javafx.ui.editing.NavigationController;

public interface PanelController {
    public boolean canExit();
    public void onExit();
    public void onEnter();
}
