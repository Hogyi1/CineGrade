package hu.elte.ik.thesis.cinegrade.app.scope;

import hu.elte.ik.thesis.cinegrade.domain.catalog.Catalog;

public record CatalogScope(ResourceStack resources, Catalog catalog) implements AutoCloseable {

    @Override
    public void close() {
        resources.close();
    }
}
