package hu.elte.ik.thesis.cinegrade.domain.exceptions;

import hu.elte.ik.thesis.cinegrade.domain.enums.ErrorCode;

public class ImportException extends CineGradeException {
    public ImportException(ErrorCode errorCode) {
        super(errorCode);
    }
}
