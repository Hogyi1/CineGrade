package hu.elte.ik.thesis.cinegrade.domain.enums;

public enum WorkspacePanel {
    IMPORT("/fxmls/importPanel.fxml"),
    DEVELOPMENT("/fxmls/developmentPanel.fxml"),
    EXPORT("/fxmls/exportPanel.fxml");

    private final String fxmlPath;
    WorkspacePanel(String fxmlPath) {
        this.fxmlPath = fxmlPath;
    }

    public String getFxmlPath() {
        return fxmlPath;
    }
}
