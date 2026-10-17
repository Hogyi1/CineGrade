package hu.elte.ik.thesis.cinegrade.app.managers.editing;

import hu.elte.ik.thesis.cinegrade.domain.catalog.Catalog;

import java.sql.Connection;

public class EditingManager implements AutoCloseable {
    public EditingManager(Connection connection, Catalog catalog) {
    }

    @Override
    public void close() {
    }
}
