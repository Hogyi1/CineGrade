package hu.elte.ik.thesis.cinegrade.domain.results;

public record AppInitResult(boolean isSuccess, String message, Throwable exception) {
    public static AppInitResult success(String message) {
        return new AppInitResult(true, message, null);
    }

    public static AppInitResult failure(String message, Throwable exception) {
        return new AppInitResult(false, message, exception);
    }

    public static AppInitResult failure(String message) {
        return new AppInitResult(false, message, null);
    }
}
