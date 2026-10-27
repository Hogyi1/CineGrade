package hu.elte.ik.thesis.cinegrade.infra.services.storage;

import hu.elte.ik.thesis.cinegrade.domain.editing.PhotoBlob;
import hu.elte.ik.thesis.cinegrade.domain.editing.PhotoEntry;
import hu.elte.ik.thesis.cinegrade.domain.enums.BlobKind;
import hu.elte.ik.thesis.cinegrade.domain.enums.ErrorCode;
import hu.elte.ik.thesis.cinegrade.domain.exceptions.CineGradeException;
import hu.elte.ik.thesis.cinegrade.infra.database.dao.preview.PhotoBlobDao;
import hu.elte.ik.thesis.cinegrade.infra.utils.thumbnail.ThumbnailFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Thumbnails of one catalog: memory cache → preview database → ThumbnailFactory (read-through).
 * <p>
 * It is the only user of the preview database, so its lock is also the preview DB's write lock.
 * The lock is only held for the quick parts (cache and DB access); generating a thumbnail
 * (~40 ms in LibRaw/Thumbnailator) always runs outside it.
 */
public class ThumbnailService implements AutoCloseable {

    private static final Logger logger = LogManager.getLogger(ThumbnailService.class);
    private static final long MAX_CACHED = 1500;

    private final PhotoBlobDao dao;
    private final ThumbnailFactory thumbnailFactory;
    private final ExecutorService cpuPool;
    private final Object lock = new Object();
    private final LinkedHashMap<String, byte[]> cache = new LinkedHashMap<>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, byte[]> eldest) {
            return size() > MAX_CACHED;
        }
    };

    public ThumbnailService(PhotoBlobDao dao, ThumbnailFactory thumbnailFactory, ExecutorService cpuPool) {
        this.dao = dao;
        this.thumbnailFactory = thumbnailFactory;
        this.cpuPool = cpuPool;
    }

    /**
     * Async version for the UI: runs {@link #getOrCreate} on the CPU pool and returns immediately.
     * Use the result with {@code thenAcceptAsync(bytes -> ..., Platform::runLater)}.
     */
    public CompletableFuture<Optional<byte[]>> load(PhotoEntry photo) {
        return CompletableFuture.supplyAsync(() -> getOrCreate(photo), cpuPool);
    }

    /**
     * Generates thumbnails for many photos in parallel on the CPU pool and waits for all of them.
     * Photos that fail are left out (and logged), they are healed later by {@link #getOrCreate}.
     * <p>
     * Blocks, so call it from the task pool, never from the CPU pool itself: a CPU thread waiting
     * for jobs queued behind it on the same pool could deadlock.
     *
     * @return photo id → JPEG bytes, only for the photos that succeeded
     */
    public Map<String, byte[]> createAll(List<PhotoEntry> photos) {

        // one job per photo
        List<CompletableFuture<Map.Entry<String, byte[]>>> futures = photos.stream()
                .map(photo -> CompletableFuture
                        .supplyAsync(() -> Map.entry(photo.getId(), create(photo)), cpuPool)
                        .exceptionally(ex -> {
                            logger.warn("Thumbnail failed for {}", photo.getFile(), ex);
                            return null;
                        }))
                .toList();

        // wait for all of them
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

        Map<String, byte[]> result = new HashMap<>();
        for (var future : futures) {
            var entry = future.join();
            if (entry != null) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    /**
     * Blocking: memory → preview DB → generate → save. Never call it on the FX thread.
     *
     * @return the JPEG bytes, or empty if the photo's file is missing or can't be decoded
     */
    public Optional<byte[]> getOrCreate(PhotoEntry photo) {
        String id = photo.getId();

        synchronized (lock) {
            byte[] cached = cache.get(id);
            if (cached != null) {
                return Optional.of(cached);
            }
            Optional<PhotoBlob> stored = find(id);
            if (stored.isPresent()) {
                byte[] data = stored.get().getData();
                cache.put(id, data);
                return Optional.of(data);
            }
        }

        if (photo.isMissingPhoto()) {
            return Optional.empty();
        }
        try {
            byte[] bytes = create(photo);
            put(id, bytes);
            return Optional.of(bytes);
        } catch (CineGradeException ex) {
            logger.warn("Could not create thumbnail for {}", photo.getFile(), ex);
            return Optional.empty();
        }
    }

    /**
     * Stores many thumbnails in ONE transaction (e.g. one import batch), then caches them.
     */
    public void putAll(Map<String, byte[]> thumbnailsByPhotoId) {
        if (thumbnailsByPhotoId == null || thumbnailsByPhotoId.isEmpty()) {
            return;
        }

        List<PhotoBlob> blobs = thumbnailsByPhotoId.entrySet().stream()
                .map(entry -> toBlob(entry.getKey(), entry.getValue()))
                .toList();

        synchronized (lock) {
            try {
                dao.upsertAll(blobs);
            } catch (SQLException ex) {
                throw new CineGradeException(ErrorCode.DB_EXECUTION_FAILED, ex, "thumbnail batch upsert");
            }
            cache.putAll(thumbnailsByPhotoId);
        }
    }

    /**
     * Stores one thumbnail in the preview database and the cache.
     */
    public void put(String photoId, byte[] jpeg) {
        synchronized (lock) {
            try {
                dao.upsert(toBlob(photoId, jpeg));
                cache.put(photoId, jpeg);
            } catch (SQLException ex) {
                throw new CineGradeException(ErrorCode.DB_EXECUTION_FAILED, ex, "thumbnail upsert " + photoId);
            }
        }
    }

    /**
     * Removes every blob of a photo. There is no foreign key across the two database files,
     * so this must be called whenever a photo is deleted from the catalog.
     */
    public void delete(String photoId) {
        synchronized (lock) {
           cache.remove(photoId);
            try {
                dao.deleteAll(photoId);
            } catch (SQLException ex) {
                throw new CineGradeException(ErrorCode.DB_EXECUTION_FAILED, ex, "thumbnail delete " + photoId);
            }
        }
    }

    /**
     * Creates a thumbnail for the given photo.
     */
    private byte[] create(PhotoEntry photo) {
        return photo.isRaw()
                ? thumbnailFactory.fromRaw(photo.getFile(), photo.getRotation())
                : thumbnailFactory.fromImage(photo.getFile(), photo.getRotation());
    }

    /**
     * Caller must hold the lock.
     */
    private Optional<PhotoBlob> find(String photoId) {
        try {
            return dao.find(photoId, BlobKind.THUMBNAIL);
        } catch (SQLException ex) {
            throw new CineGradeException(ErrorCode.DB_EXECUTION_FAILED, ex, "thumbnail lookup " + photoId);
        }
    }

    @Override
    public void close() {
        synchronized (lock) {
            cache.clear();
        }
    }

    private PhotoBlob toBlob(String photoId, byte[] jpeg) {
        return PhotoBlob.builder()
                .photoId(photoId)
                .kind(BlobKind.THUMBNAIL)
                .mime(ThumbnailFactory.MIME_TYPE)
                .data(jpeg)
                .build();
    }
}
