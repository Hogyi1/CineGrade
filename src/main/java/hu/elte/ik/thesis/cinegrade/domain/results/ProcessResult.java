package hu.elte.ik.thesis.cinegrade.domain.results;

public record ProcessResult(int exitCode, String stdout, String stderr) {

    public static ProcessResult success(String stdout) {
        return new ProcessResult(0, stdout, "");
    }

    public static ProcessResult fail(int exitCode, String stderr) {
        return new ProcessResult(exitCode, "", stderr);
    }

    public boolean isSuccess() {
        return exitCode == 0;
    }
}
