package hu.elte.ik.thesis.cinegrade.domain.error;

import hu.elte.ik.thesis.cinegrade.domain.exceptions.CineGradeException;

public interface ErrorListener {
    void onError(CineGradeException exception, ErrorHandler.ErrorDisplayMode displayMode);
}
