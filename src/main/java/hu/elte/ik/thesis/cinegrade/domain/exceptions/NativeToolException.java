package hu.elte.ik.thesis.cinegrade.domain.exceptions;

import hu.elte.ik.thesis.cinegrade.domain.enums.ErrorCode;

public class NativeToolException extends CineGradeException {
    public NativeToolException(ErrorCode errorCode) {
        super(errorCode);
    }
}
