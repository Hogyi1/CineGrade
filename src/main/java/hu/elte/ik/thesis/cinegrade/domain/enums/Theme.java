package hu.elte.ik.thesis.cinegrade.domain.enums;

public enum Theme {
    DARK("theme-dark"),
    LIGHT("theme-light");

    private final String styleClass;
    Theme(String styleClass) { this.styleClass = styleClass; }
    public String getStyleClass() { return styleClass; }
}
