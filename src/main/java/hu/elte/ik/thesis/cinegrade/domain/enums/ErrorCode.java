package hu.elte.ik.thesis.cinegrade.domain.enums;

public enum ErrorCode {

    // ==========================================
    // 1000–1099: Startup / RequirementChecker
    // ==========================================
    REQ_LIBRAW_DLL_MISSING(1001, "LibRaw native library (libraw.dll) not found at expected path: %s", Severity.FATAL),
    REQ_LIBRAW_LOAD_FAILED(1002, "Failed to load LibRaw native library: %s", Severity.FATAL),
    REQ_FFMPEG_NOT_FOUND(1003, "FFmpeg executable not found on PATH or configured location.", Severity.FATAL),
    REQ_FFPROBE_NOT_FOUND(1004, "FFprobe executable not found on PATH or configured location.", Severity.FATAL),
    REQ_EXIFTOOL_NOT_FOUND(1005, "ExifTool executable not found on PATH or configured location.", Severity.FATAL),
    REQ_APP_DATA_DIR_NOT_WRITABLE(1006, "App data directory (%s) is not writable.", Severity.FATAL),
    REQ_RESOURCE_MISSING(1007, "Required application resource is missing: %s", Severity.FATAL),
    REQ_OPENGL_UNSUPPORTED(1008, "OpenGL 3.3+ context could not be created or is unsupported by hardware.", Severity.FATAL),
    REQ_JAVA_VERSION_INCOMPATIBLE(1009, "Incompatible Java runtime version. Java 21 or higher is required.", Severity.FATAL),

    // ==========================================
    // 1100–1199: Native Tools & Process Execution
    // ==========================================
    PROCESS_EXECUTION_FAILED(1101, "Failed to execute native process: %s", Severity.ERROR),
    PROCESS_TIMEOUT(1102, "Native process execution timed out after %d ms: %s", Severity.ERROR),
    PROCESS_EXITED_WITH_ERROR(1103, "Native process '%s' exited with error code %d: %s", Severity.ERROR),
    LIBRAW_DECODE_FAILED(1110, "LibRaw failed to decode RAW image (code %d): %s", Severity.ERROR),
    LIBRAW_UNPACK_FAILED(1111, "LibRaw failed to unpack image data (code %d).", Severity.ERROR),
    LIBRAW_THUMBNAIL_EXTRACTION_FAILED(1112, "Failed to extract embedded thumbnail via LibRaw (code %d).", Severity.ERROR),
    EXIFTOOL_EXTRACTION_FAILED(1120, "Failed to extract metadata via ExifTool: %s", Severity.ERROR),
    EXIFTOOL_WRITE_FAILED(1121, "Failed to write metadata via ExifTool: %s", Severity.ERROR),
    FFMPEG_ENCODE_FAILED(1130, "FFmpeg failed to encode image output: %s", Severity.ERROR),
    FFPROBE_PROBE_FAILED(1131, "FFprobe failed to analyze media file: %s", Severity.ERROR),

    // ==========================================
    // 2000–2099: Catalog Operations
    // ==========================================
    CATALOG_CREATE_FAILED(2001, "Failed to create catalog at path: %s", Severity.ERROR),
    CATALOG_NOT_FOUND(2002, "Catalog not found at path: %s", Severity.ERROR),
    CATALOG_ALREADY_EXISTS(2003, "Catalog already exists at path: %s", Severity.ERROR),
    CATALOG_CORRUPTED(2004, "Catalog manifest file is corrupted or unreadable: %s", Severity.ERROR),
    CATALOG_SAVE_FAILED(2005, "Failed to save catalog manifest: %s", Severity.ERROR),
    CATALOG_DELETE_FAILED(2006, "Failed to delete catalog directory: %s", Severity.ERROR),
    CATALOG_LOCKED(2007, "Catalog is currently locked by another process or operation.", Severity.ERROR),

    // ==========================================
    // 2100–2199: File Management & Import
    // ==========================================
    FILE_NOT_FOUND(2101, "File not found: %s", Severity.ERROR),
    FILE_READ_FAILED(2102, "Failed to read file: %s", Severity.ERROR),
    FILE_WRITE_FAILED(2103, "Failed to write file: %s", Severity.ERROR),
    FILE_DELETE_FAILED(2104, "Failed to delete file: %s", Severity.WARNING),
    UNSUPPORTED_FILE_FORMAT(2105, "Unsupported image or media file format: %s", Severity.ERROR),
    IMPORT_COPY_FAILED(2110, "Failed to copy original photo into catalog storage: %s", Severity.ERROR),
    IMPORT_THUMBNAIL_FAILED(2111, "Failed to generate or save thumbnail for photo: %s", Severity.WARNING),
    IMPORT_METADATA_FAILED(2112, "Failed to parse metadata during import for file: %s", Severity.WARNING),
    FOLDER_CREATE_FAILED(2120, "Failed to create virtual folder '%s'.", Severity.ERROR),
    FOLDER_DUPLICATE_NAME(2121, "A folder with name '%s' already exists in this location.", Severity.WARNING),
    HIERARCHY_EXPORT_FAILED(2130, "Failed to export physical folder hierarchy to destination: %s", Severity.ERROR),

    // ==========================================
    // 3000–3099: Editing & Presets / LUTs
    // ==========================================
    EDIT_STATE_CORRUPTED(3001, "Edit state data is corrupted or contains invalid parameters.", Severity.ERROR),
    PRESET_SAVE_FAILED(3002, "Failed to save preset to disk: %s", Severity.ERROR),
    PRESET_LOAD_FAILED(3003, "Failed to load preset from disk: %s", Severity.ERROR),
    PRESET_DELETE_FAILED(3004, "Failed to delete preset: %s", Severity.ERROR),
    PRESET_NOT_FOUND(3005, "Preset not found: %s", Severity.ERROR),
    LUT_FILE_INVALID(3010, "Invalid or corrupted 3D LUT (.cube) file: %s", Severity.ERROR),
    LUT_GENERATION_FAILED(3011, "Failed to generate 3D LUT from edit state: %s", Severity.ERROR),

    // ==========================================
    // 4000–4099: Export Pipeline
    // ==========================================
    EXPORT_DESTINATION_UNWRITABLE(4001, "Export destination directory is not writable or disk is full: %s", Severity.ERROR),
    EXPORT_RENDER_FAILED(4002, "Failed to render graded image for export: %s", Severity.ERROR),
    EXPORT_INVALID_SETTINGS(4003, "Invalid export settings specified: %s", Severity.ERROR),
    EXPORT_CANCELLED(4004, "Export job was cancelled by user.", Severity.WARNING),
    EXPORT_QUEUE_ERROR(4005, "Export queue encountered an unrecoverable error.", Severity.ERROR),

    // ==========================================
    // 5000–5099: Rendering & OpenGL Engine
    // ==========================================
    OPENGL_INIT_FAILED(5001, "Failed to initialize OpenGL engine or context.", Severity.FATAL),
    SHADER_COMPILATION_FAILED(5002, "GLSL shader compilation failed: %s", Severity.FATAL),
    SHADER_LINK_FAILED(5003, "GLSL shader program linking failed: %s", Severity.FATAL),
    FBO_CREATION_FAILED(5004, "Failed to create or resize OpenGL FrameBuffer Object (status %d).", Severity.ERROR),
    TEXTURE_UPLOAD_FAILED(5005, "Failed to upload image pixel data to OpenGL texture.", Severity.ERROR),
    RENDER_TARGET_READBACK_FAILED(5006, "Failed to read back rendered pixels from FBO to CPU buffer.", Severity.ERROR),

    // ==========================================
    // 8000–8099: SQL Errors
    // ==========================================
    FAILED_TO_CREATE_INDEX(8001, "Failed to create index: %s", Severity.ERROR),
    FAILED_TO_CREATE_TABLE(8002, "Failed to create table: %s", Severity.ERROR),
    DB_CONNECTION_FAILED(8003, "Failed to establish database connection: %s", Severity.FATAL),
    DB_EXECUTION_FAILED(8004, "Failed to execute database statement: %s", Severity.ERROR),
    DB_TRANSACTION_FAILED(8005, "Database transaction failed: %s", Severity.ERROR),
    DB_SCHEMA_INIT_FAILED(8006, "Failed to initialize database schema: %s", Severity.FATAL),
    DB_CLOSE_FAILED(8007, "Failed to close database connection cleanly: %s", Severity.WARNING),
    DB_RECORD_NOT_FOUND(8008, "Database record not found: %s", Severity.WARNING),
    DB_LOCKED(8009, "Database is locked or busy: %s", Severity.ERROR),
    DB_CORRUPTED(8010, "Database file is corrupted or unreadable: %s", Severity.FATAL),

    // ==========================================
    // 9000–9099: Unexpected & General Errors
    // ==========================================
    UNKNOWN_ERROR(9000, "An unknown error occurred: %s", Severity.ERROR),
    OPERATION_CANCELLED(9001, "Operation was cancelled.", Severity.WARNING),
    TASK_EXECUTION_FAILED(9002, "Background task execution failed: %s", Severity.ERROR),
    JSON_SERIALIZATION_FAILED(9003, "Failed to serialize object to JSON: %s", Severity.ERROR),
    JSON_DESERIALIZATION_FAILED(9004, "Failed to deserialize JSON content: %s", Severity.ERROR);

    private final int code;
    private final String message;
    private final Severity severity;

    ErrorCode(int code, String message, Severity severity) {
        this.code = code;
        this.message = message;
        this.severity = severity;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String format(Object... args) {
        if (args == null || args.length == 0) {
            return message;
        }
        return String.format(message, args);
    }

    @Override
    public String toString() {
        return String.format("[%d - %s] %s", code, severity, message);
    }
}
