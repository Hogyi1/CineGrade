package hu.elte.ik.thesis.cinegrade.javafx.ui.editing;

import hu.elte.ik.thesis.cinegrade.domain.context.CatalogContext;
import hu.elte.ik.thesis.cinegrade.javafx.ui.PanelController;

public class DevelopmentPanelController implements PanelController {

    private final CatalogContext catalogContext;

    public DevelopmentPanelController(CatalogContext catalogContext) {
        this.catalogContext = catalogContext;
    }

    @Override
    public void setNavigationController(NavigationController navigationController) {

    }

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
