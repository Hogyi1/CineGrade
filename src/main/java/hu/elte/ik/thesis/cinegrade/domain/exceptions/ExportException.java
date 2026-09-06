package hu.elte.ik.thesis.cinegrade.domain.exceptions;

import hu.elte.ik.thesis.cinegrade.domain.enums.ErrorCode;

public class ExportException extends CineGradeException {
    public ExportException(ErrorCode errorCode) {
        super(errorCode);
    }
}
