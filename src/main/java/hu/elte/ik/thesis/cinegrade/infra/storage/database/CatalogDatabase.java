package hu.elte.ik.thesis.cinegrade.infra.storage.database;

import hu.elte.ik.thesis.cinegrade.infra.storage.database.schema.CatalogSchemaInitializer;

import java.nio.file.Path;

/**
 * The catalog itself: the .cgproj file (photos, metadata, edit states, presets, catalog_info).
 */
public class CatalogDatabase extends SqliteDatabase {

    public CatalogDatabase(Path projectFilePath) {
        super(projectFilePath, CatalogSchemaInitializer::initialize);
    }
}
