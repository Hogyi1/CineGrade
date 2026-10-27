package hu.elte.ik.thesis.cinegrade.infra.utils.thumbnail;

import hu.elte.ik.thesis.cinegrade.domain.enums.ErrorCode;
import hu.elte.ik.thesis.cinegrade.domain.enums.PhotoExtensions;
import hu.elte.ik.thesis.cinegrade.domain.exceptions.CineGradeException;
import hu.elte.ik.thesis.cinegrade.infra.services.libraw.LibrawService;
import net.coobird.thumbnailator.Thumbnails;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

// [AI-GENERATED JAVADOC]

/**
 * Builds the JPEG thumbnails that are stored as BLOBs in the preview database.
 * <p>
 * Thumbnails are produced either from the preview embedded in a RAW file (via {@link LibrawService})
 * or from a regular image file. In both cases the result is scaled to fit within
 * {@value #MAX_EDGE}x{@value #MAX_EDGE} pixels, keeping the aspect ratio, and encoded as JPEG.
 * <p>
 * The factory holds no state, so a single instance can be shared between threads.
 */
public class ThumbnailFactory {

    /**
     * Maximum width and height, in pixels, of a generated thumbnail.
     */
    public static final int MAX_EDGE = 512;

    /**
     * MIME type of the bytes returned by this factory.
     */
    public static final String MIME_TYPE = "image/jpeg";

    /**
     * JPEG encoder quality, from 0.0 (lowest) to 1.0 (highest).
     */
    public static final double JPEG_QUALITY = 0.75;

    /**
     * Creates a thumbnail from the preview embedded in a RAW file.
     * <p>
     * The embedded preview is decoded directly when it is a JPEG. Otherwise it is treated as
     * 8-bit interleaved RGB bitmap data. A new {@link LibrawService} is opened and closed for each call.
     *
     * @param rawFile  the RAW file to read the embedded preview from
     * @param rotation clockwise rotation to apply, in degrees
     * @return the thumbnail encoded as JPEG
     * @throws CineGradeException with {@link ErrorCode#IMPORT_THUMBNAIL_FAILED} if the file has no
     *                            embedded preview, the preview cannot be decoded, or encoding fails
     */
    public byte[] fromRaw(File rawFile, int rotation) {
        try (LibrawService librawService = new LibrawService()) {
            LibrawService.RawThumbnail thumbnail = librawService.extractThumbnail(rawFile)
                    .orElseThrow(() -> new CineGradeException(ErrorCode.IMPORT_THUMBNAIL_FAILED, rawFile));

            BufferedImage source = thumbnail.type() == 2
                    ? toImage(thumbnail)
                    : ImageIO.read(new ByteArrayInputStream(thumbnail.data()));
            if (source == null) {
                throw new CineGradeException(ErrorCode.IMPORT_THUMBNAIL_FAILED, rawFile);
            }

            return encode(Thumbnails.of(source).rotate(rotation));
        } catch (Exception ex) {
            throw new CineGradeException(ErrorCode.IMPORT_THUMBNAIL_FAILED, ex, rawFile);
        }
    }

    /**
     * Creates a thumbnail from a regular image file (JPEG, PNG, etc.).
     *
     * @param imageFile the image file to read
     * @param rotation  clockwise rotation to apply, in degrees
     * @return the thumbnail encoded as JPEG
     * @throws CineGradeException with {@link ErrorCode#IMPORT_THUMBNAIL_FAILED} if the file cannot be
     *                            read, decoded or encoded
     */
    public byte[] fromImage(File imageFile, int rotation) {
        try {
            return encode(Thumbnails.of(imageFile).rotate(rotation));
        } catch (IOException ex) {
            throw new CineGradeException(ErrorCode.IMPORT_THUMBNAIL_FAILED, ex, imageFile);
        }
    }

    /**
     * Wraps the raw bitmap of a LibRaw thumbnail in a {@link BufferedImage} without copying the pixel data.
     * The data is expected to be 8-bit interleaved RGB, three bytes per pixel.
     *
     * @param thumbnail the bitmap thumbnail returned by LibRaw
     * @return an sRGB image backed by the thumbnail's byte array
     * @throws CineGradeException with {@link ErrorCode#IMPORT_THUMBNAIL_FAILED} if the bit depth is not 8
     *                            or the dimensions are not positive
     */
    private BufferedImage toImage(LibrawService.RawThumbnail thumbnail) {
        if (thumbnail.type() == 1) {
            byte[] jpegBytes = thumbnail.data();
            if (jpegBytes == null || jpegBytes.length == 0) {
                throw new CineGradeException(ErrorCode.IMPORT_THUMBNAIL_FAILED, "Empty JPEG thumbnail data");
            }

            try {
                BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(jpegBytes));
                if (decoded == null) {
                    throw new CineGradeException(ErrorCode.IMPORT_THUMBNAIL_FAILED, "Failed to decode JPEG thumbnail");
                }
                return decoded;
            } catch (IOException e) {
                throw new CineGradeException(ErrorCode.IMPORT_THUMBNAIL_FAILED, "Error reading JPEG thumbnail", e);
            }
        }

        // type == 2: Raw 8-bit interleaved RGB bitmap
        if (thumbnail.type() == 2) {
            if (thumbnail.bits() != 8 || thumbnail.width() <= 0 || thumbnail.height() <= 0) {
                throw new CineGradeException(ErrorCode.IMPORT_THUMBNAIL_FAILED, "Invalid thumbnail bitmap dimensions or bit depth");
            }

            int width = thumbnail.width();
            int height = thumbnail.height();
            byte[] data = thumbnail.data();

            WritableRaster raster = Raster.createInterleavedRaster(
                    new DataBufferByte(data, data.length),
                    width,
                    height,
                    width * 3, // Jump a row
                    3, // Per color R, G, B
                    new int[]{0, 1, 2}, // sorrend
                    null
            );

            ColorModel cm = new ComponentColorModel(
                    ColorSpace.getInstance(ColorSpace.CS_sRGB),
                    false,
                    false,
                    Transparency.OPAQUE,
                    DataBuffer.TYPE_BYTE
            );

            return new BufferedImage(cm, raster, false, null);
        }

        throw new CineGradeException(ErrorCode.IMPORT_THUMBNAIL_FAILED, "Unsupported thumbnail format type: " + thumbnail.type());
    }

    /**
     * Applies the shared output settings ({@link #JPEG_QUALITY}, JPEG format, {@link #MAX_EDGE} bounding
     * box) to the builder and renders it into a byte array.
     *
     * @param builder the configured Thumbnailator source, with any rotation already applied
     * @return the encoded JPEG bytes
     * @throws IOException if rendering or encoding fails
     */
    private byte[] encode(Thumbnails.Builder<?> builder) throws IOException {
        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        builder.outputQuality(JPEG_QUALITY)
                .outputFormat(PhotoExtensions.JPEG.getExtension())
                .size(MAX_EDGE, MAX_EDGE)
                .allowOverwrite(true)
                .toOutputStream(bao);

        return bao.toByteArray();
    }
}
