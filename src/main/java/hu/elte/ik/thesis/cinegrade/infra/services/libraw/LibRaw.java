package hu.elte.ik.thesis.cinegrade.infra.services.libraw;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import hu.elte.ik.thesis.cinegrade.infra.process.NativeBinaryLocator;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

public interface LibRaw extends Library {

    String path = NativeBinaryLocator.getBinaryPath("libraw.dll").get().toString();
    LibRaw INSTANCE = Native.load(path, LibRaw.class);

    // ── Core structs ──────────────────────────────────────────────────────

    @Structure.FieldOrder({
            "type", "height", "width", "colors", "bits", "data_size"
    })
    class ProcessedImage extends Structure {
        public int    type;
        public short  height;
        public short  width;
        public short  colors;
        public short  bits;
        public int    data_size;
        private ByteBuffer data;
        // data[1] is NOT mapped — we read it manually via pointer arithmetic

        public static class ByReference
                extends ProcessedImage implements Structure.ByReference {}

        /**
         * Reads pixel bytes starting immediately after the struct fields.
         * data[1] in C means the array lives contiguously after data_size in memory.
         */
        public void setPixelBytes() {
            if (data_size <= 0) return;

            // Calculate offset: sum of all field sizes
            // enum(4) + ushort*4(8) + unsigned int(4) = 16 bytes
            int dataOffset = 16;


            if (data == null){
                data = ByteBuffer.allocateDirect(data_size);
                data.put(getPointer().getByteArray(dataOffset, data_size));
                data.flip();
            }
        }

        public ByteBuffer getByteBuffer(){return data;}
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────
    Pointer libraw_init(int flags);
    void    libraw_recycle(Pointer lr);
    void    libraw_close(Pointer lr);

    // ── File I/O ──────────────────────────────────────────────────────────
    int libraw_open_file(Pointer lr, String filename);

    // ── Processing ────────────────────────────────────────────────────────
    int libraw_unpack(Pointer lr);
    int libraw_unpack_thumb(Pointer lr);
    int libraw_dcraw_process(Pointer lr);

    // ── Output ────────────────────────────────────────────────────────────
    ProcessedImage.ByReference libraw_dcraw_make_mem_image(Pointer lr, int[] errc);
    ProcessedImage.ByReference libraw_dcraw_make_mem_thumb(Pointer lr, int[] errc);
    void libraw_dcraw_clear_mem(Pointer img);

    // ── Settings ──────────────────────────────────────────────────────────
    void libraw_set_demosaic(Pointer lr, int value);
    void libraw_set_output_bps(Pointer lr, int value);
    void libraw_set_output_color(Pointer lr, int value);
    void libraw_set_no_auto_bright(Pointer lr, int value);
    void libraw_set_use_camera_wb(Pointer lr, int value);
    //void libraw_set_output_rotate(Pointer lr, int value);

    void libraw_set_user_mul(Pointer lr, int value, float value2);
    // Others
    String libraw_version();

    // ── Metadata getters ──────────────────────────────────────────────────
    int libraw_get_iwidth(Pointer lr);
    int libraw_get_iheight(Pointer lr);
}