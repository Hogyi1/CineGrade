package hu.elte.ik.thesis.cinegrade.javafx.ui.editing;

import hu.elte.ik.thesis.cinegrade.app.scope.CatalogScope;
import hu.elte.ik.thesis.cinegrade.javafx.ui.PanelController;

public class ExportPanelController implements PanelController {

    private final CatalogScope catalogScope;

    public ExportPanelController(CatalogScope catalogScope) {
        this.catalogScope = catalogScope;
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
