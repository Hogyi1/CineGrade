package hu.elte.ik.thesis.cinegrade.domain.error;

import hu.elte.ik.thesis.cinegrade.domain.enums.ErrorCode;
import hu.elte.ik.thesis.cinegrade.domain.exceptions.CineGradeException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class ErrorHandler implements Thread.UncaughtExceptionHandler {

    private static final ErrorHandler INSTANCE = new ErrorHandler();
    private static final Logger logger = LogManager.getLogger(ErrorHandler.class);

    private final Set<ErrorListener> listeners = new CopyOnWriteArraySet<>();

    public void registerListener(ErrorListener listener){
        logger.info("Registered listener for errors: {}", listener);
        listeners.add(listener);
    }

    private ErrorHandler() {
    }

    public static ErrorHandler getInstance() {
        return INSTANCE;
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        if (e instanceof CineGradeException cgex) {
            handle(cgex);
        } else if (e.getCause() instanceof CineGradeException cgex) {
            handle(cgex);
        } else {
            handle(e);
        }
    }

    public void install() {
        Thread.setDefaultUncaughtExceptionHandler(INSTANCE);
        Thread.currentThread().setUncaughtExceptionHandler(INSTANCE);
    }

    public void handle(CineGradeException exception) {
        logger.error("CineGradeException [{}]: {}", exception.getErrorCodeValue(), exception.getErrorMessage(), exception);

        switch (exception.getSeverity()) {
            case FATAL -> listeners.forEach(listener -> listener.onError(exception, ErrorDisplayMode.MODAL));
            case ERROR, WARNING -> listeners.forEach(listener -> listener.onError(exception, ErrorDisplayMode.TOAST));
        }
    }

    public void handle(Throwable exception) {
        if (exception instanceof CineGradeException cgex) {
            handle(cgex);
            return;
        }
        if (exception.getCause() instanceof CineGradeException cgex) {
            handle(cgex);
            return;
        }
        logger.error("Unhandled Exception: {}", exception.getMessage(), exception);
        listeners.forEach(listener -> listener.onError(new CineGradeException(ErrorCode.UNKNOWN_ERROR, exception, "Unhandled exception"), ErrorDisplayMode.TOAST));
    }

    public enum ErrorDisplayMode {
        MODAL,
        TOAST
    }
}

