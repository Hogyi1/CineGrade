package hu.elte.ik.thesis.cinegrade.domain.enums;

public enum ViewType {
    SPLASH("/fxmls/splashScreen.fxml", "CineGrade", false),
    MAIN_MENU("/fxmls/mainMenu.fxml", "CineGrade — Projects", true),
    TEST_MENU("/fxmls/Test.fxml", "CineGrade - Test panel", false);

    private final String fxmlPath;
    private final String title;
    private final boolean resizable;

    ViewType(String fxmlPath, String title, boolean resizable) {
        this.fxmlPath = fxmlPath;
        this.title = title;
        this.resizable = resizable;
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
}
