package hu.elte.ik.thesis.cinegrade.domain.exceptions;

import hu.elte.ik.thesis.cinegrade.domain.enums.ErrorCode;
import hu.elte.ik.thesis.cinegrade.domain.enums.Severity;

public class CineGradeException extends RuntimeException {

    private final ErrorCode ERROR_CODE;
    private final Severity SEVERITY;

    public CineGradeException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.ERROR_CODE = errorCode;
        this.SEVERITY = errorCode.getSeverity();
    }

    public ErrorCode getErrorCode() {
        return ERROR_CODE;
    }

    public Severity getSeverity() {
        return SEVERITY;
    }

    public String getErrorMessage() {
        return ERROR_CODE.getMessage();
    }

    public int getErrorCodeValue() {
        return ERROR_CODE.getCode();
    }
}
