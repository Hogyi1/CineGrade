package hu.elte.ik.thesis.cinegrade.infra.process;

import hu.elte.ik.thesis.cinegrade.domain.results.ProcessResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

public final class ProcessRunner {

    private static final Logger logger = LogManager.getLogger(ProcessRunner.class);
    public static ProcessResult run(List<String> commands, int timeOut, BooleanSupplier isCanceled) {

        Process p;

        logger.debug("Executing CLI command: {}", String.join(" ", commands));

        try {
            p = new ProcessBuilder().command(commands).start();
        } catch (IOException e) {
            String cmd = String.join(" ", commands);
            String msg = "Failed to start process: " + cmd + "\n" + e.getMessage();
            logger.error(msg);
            return new ProcessResult(-10, "", msg);
        }

        CompletableFuture<String> stdoutFuture = CompletableFuture.supplyAsync(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            } catch (IOException e) {
                return "";
            }
        });

        CompletableFuture<String> stderrFuture = CompletableFuture.supplyAsync(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getErrorStream(), StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            } catch (IOException e) {
                return "";
            }
        });

        long now = System.currentTimeMillis();

        try {
            while (true) {

                // Process finished either failed or succeeded
                if (p.waitFor(200, TimeUnit.MILLISECONDS)) {
                    int exitCode = p.exitValue();
                    String stdout = stdoutFuture.join();
                    String stderr = stderrFuture.join();
                    logger.debug("Command finished with exit code {} in {} ms", exitCode, (System.currentTimeMillis() - now));
                    return new ProcessResult(exitCode, stdout, stderr);
                }

                // Process is canceled or interrupted
                if ((isCanceled != null && isCanceled.getAsBoolean()) || Thread.currentThread().isInterrupted()) {
                    killProcessTree(p);
                    stderrFuture.cancel(true);
                    stdoutFuture.cancel(true);
                    logger.debug("Process was interrupted!");
                    return ProcessResult.fail(-2, "Process was interrupted!");
                }

                // Process timed out
                if (System.currentTimeMillis() - now >= TimeUnit.SECONDS.toMillis(timeOut)) {
                    killProcessTree(p);
                    stderrFuture.cancel(true);
                    stdoutFuture.cancel(true);
                    logger.debug("Process timed out after {} seconds", timeOut);
                    return ProcessResult.fail(-5, "Process timed out after " + timeOut + " seconds");
                }
            }
        } catch (Exception e) {
            killProcessTree(p);
            stderrFuture.cancel(true);
            stdoutFuture.cancel(true);
            logger.error("An unexpected error occurred while executing the process", e);
            return ProcessResult.fail(-1, "An unexpected error occurred");
        }
    }

    private static void killProcessTree(Process process) {
        process.descendants().forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
    }
}