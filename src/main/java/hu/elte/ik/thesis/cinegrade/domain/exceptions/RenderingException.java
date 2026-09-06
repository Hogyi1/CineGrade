package hu.elte.ik.thesis.cinegrade.domain.exceptions;

import hu.elte.ik.thesis.cinegrade.domain.enums.ErrorCode;

public class RenderingException extends CineGradeException {
    public RenderingException(ErrorCode errorCode) {
        super(errorCode);
    }
}
