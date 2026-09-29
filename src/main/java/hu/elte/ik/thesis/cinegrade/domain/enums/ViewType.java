package hu.elte.ik.thesis.cinegrade.domain.enums;

public enum ViewType {
    SPLASH("/fxmls/splashScreen.fxml", "CineGrade", false, false),
    MAIN_MENU("/fxmls/mainMenu.fxml", "CineGrade — Projects", true, true),
    EDIT_PAGE("/fxmls/testEditPage.fxml", "CineGrade — Edit", true, true),
    TEST_MENU("/fxmls/Test.fxml", "CineGrade - Test panel", false, false);

    private final String fxmlPath;
    private final String title;
    private final boolean resizable;
    private final boolean fullScreen;

    ViewType(String fxmlPath, String title, boolean resizable, boolean fullScreen) {
        this.fxmlPath = fxmlPath;
        this.title = title;
        this.resizable = resizable;
        this.fullScreen = fullScreen;
    }

    public String getFxmlPath() {
        return fxmlPath;
    }

    public String getTitle() {
        return title;
    }

    public boolean isResizable() {
        return resizable;
    }
    public boolean isFullScreen() {
        return fullScreen;
    }
}
