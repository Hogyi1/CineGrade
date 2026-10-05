package hu.elte.ik.thesis.cinegrade.infra.services.libraw;

import com.sun.jna.Pointer;
import hu.elte.ik.thesis.cinegrade.domain.enums.ErrorCode;
import hu.elte.ik.thesis.cinegrade.domain.exceptions.CineGradeException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

public class LibrawService {

    private static final Logger logger = LogManager.getLogger(LibrawService.class);
    private final LibRaw libRaw;

    public LibrawService() {
        this.libRaw = LibRaw.INSTANCE;
    }

    public boolean extractThumbnail(File rawFile, Path destination) {
        Pointer handler = LibRaw.INSTANCE.libraw_init(0);
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

                Files.createDirectories(destination.getParent());
                Files.write(destination, byteBuffer.array());
                logger.debug("%dx%d decoded, %d bits, %d bytes%n",
                        (int) thumbnail.width, (int) thumbnail.height, (int) thumbnail.bits,
                        thumbnail.getByteBuffer().capacity());

                libRaw.libraw_dcraw_clear_mem(thumbnail.getPointer());
            }
        } catch (IOException e) {
            throw new CineGradeException(ErrorCode.LIBRAW_THUMBNAIL_EXTRACTION_FAILED, e);
        } finally {
            libRaw.libraw_recycle(handler);
        }

         return true;
    }
}
