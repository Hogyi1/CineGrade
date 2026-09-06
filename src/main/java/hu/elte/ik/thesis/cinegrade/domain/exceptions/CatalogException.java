package hu.elte.ik.thesis.cinegrade.domain.exceptions;

import hu.elte.ik.thesis.cinegrade.domain.enums.ErrorCode;

public class CatalogException extends CineGradeException {
    public CatalogException(ErrorCode errorCode) {
        super(errorCode);
    }
}
