package hu.elte.ik.thesis.cinegrade.infra.storage.database;

import hu.elte.ik.thesis.cinegrade.infra.storage.database.schema.PreviewSchemaInitializer;

import java.nio.file.Path;

/**
 * Thumbnail and preview BLOBs of one catalog, kept in a separate SQLite file
 * (like Lightroom's "Previews.lrdata"). It is a pure cache: deleting the file loses nothing,
 * the ThumbnailService regenerates what is missing. Keeping it out of the .cgproj means
 * the catalog stays small and a broken/huge cache never endangers the edits.
 */
public class PreviewDatabase extends SqliteDatabase {

    public PreviewDatabase(Path previewDatabasePath) {
        super(previewDatabasePath, PreviewSchemaInitializer::initialize);
    }
}
