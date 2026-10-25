package hu.elte.ik.thesis.cinegrade.infra.services.libraw;

import com.sun.jna.Pointer;
import hu.elte.ik.thesis.cinegrade.domain.enums.ErrorCode;
import hu.elte.ik.thesis.cinegrade.domain.exceptions.CineGradeException;
import hu.elte.ik.thesis.cinegrade.infra.services.StorageService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

public class LibrawService implements AutoCloseable {

    private static final Logger logger = LogManager.getLogger(LibrawService.class);
    private final LibRaw libRaw;
    private final StorageService storageService;
    private final Pointer handler;
    private volatile boolean closed = false;

    public LibrawService() {
        this.libRaw = LibRaw.INSTANCE;
        this.storageService = new StorageService();

        this.handler = libRaw.libraw_init(0);
        if (this.handler == null) {
            logger.error("Failed to initialize native LibRaw context");
            throw new CineGradeException(ErrorCode.REQ_LIBRAW_LOAD_FAILED, "Failed to initialize LibRaw native handler");
        }
    }

    public boolean extractThumbnail(File rawFile, Path destinationFile) {
        if (closed) return false;

        LibRaw.ProcessedImage.ByReference thumbnail;
        if (handler == null) {
            logger.error("Failed to initialize libraw");
            return false;
        }

        try {
            int err = libRaw.libraw_open_file(handler, rawFile.getAbsolutePath());
            if (err != 0) {
                logger.error("Failed to open raw file: {}", rawFile.getAbsolutePath());
                return false;
            }

            int[] errc = {0};

            libRaw.libraw_unpack_thumb(handler);
            thumbnail = libRaw.libraw_dcraw_make_mem_thumb(handler, errc);

            if (thumbnail != null) {
                thumbnail.read();
                thumbnail.setPixelBytes();

                ByteBuffer byteBuffer = thumbnail.getByteBuffer();
                if (byteBuffer == null) {
                    logger.error("Failed to get byte buffer for thumbnail");
                    return false;
                }

                byteBuffer.rewind();
                byte[] jpegBytes = new byte[byteBuffer.remaining()];
                byteBuffer.get(jpegBytes);

                storageService.ensureDirectory(destinationFile.getParent());
                Files.write(destinationFile, jpegBytes);
                if (thumbnail.type == 2) {
                    logger.debug("Embedded JPEG thumbnail extracted: {} bytes ({} KB)",
                            thumbnail.data_size, thumbnail.data_size / 1024);
                } else {
                    logger.debug("{}x{} bitmap thumbnail decoded, {} bits, {} bytes",
                            (int) thumbnail.width, (int) thumbnail.height, (int) thumbnail.bits, thumbnail.data_size);
                }

                libRaw.libraw_dcraw_clear_mem(thumbnail.getPointer());
            }
        } catch (IOException e) {
            throw new CineGradeException(ErrorCode.LIBRAW_THUMBNAIL_EXTRACTION_FAILED, e);
        } finally {
            libRaw.libraw_recycle(handler);
        }

         return true;
    }

    @Override
    public void close() throws Exception {
        if (!closed) {
            libRaw.libraw_close(handler);
            closed = true;
            logger.debug("LibRaw handler closed");
        }
    }
}
