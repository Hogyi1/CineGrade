package hu.elte.ik.thesis.cinegrade.domain.exceptions;

import hu.elte.ik.thesis.cinegrade.domain.enums.ErrorCode;

public class EditingException extends CineGradeException {
    public EditingException(ErrorCode errorCode) {
        super(errorCode);
    }
}
