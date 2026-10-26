package hu.elte.ik.thesis.cinegrade.app.scope;

import hu.elte.ik.thesis.cinegrade.domain.enums.ErrorCode;
import hu.elte.ik.thesis.cinegrade.domain.exceptions.CineGradeException;

import java.util.ArrayDeque;
import java.util.Deque;

public class ResourceStack implements AutoCloseable {

    private final Deque<AutoCloseable> stack = new ArrayDeque<>();

    public <T extends AutoCloseable> T push(T resource) {
        stack.push(resource);
        return resource;
    }

    /**
     * Closes all resources in the stack.
     */
    @Override
    public void close() {
        Exception firstException = null;
        while (!stack.isEmpty()) {
            try {
                stack.pop().close();
            } catch (Exception e) {
                if (firstException == null) {
                    firstException = e;
                } else {
                    firstException.addSuppressed(e);
                }
            }
        }
        if (firstException != null) {
            throw new CineGradeException(ErrorCode.UNKNOWN_ERROR, firstException, "Failed to close some resources");
        }
    }
}
