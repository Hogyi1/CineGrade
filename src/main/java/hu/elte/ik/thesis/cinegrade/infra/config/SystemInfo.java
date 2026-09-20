package hu.elte.ik.thesis.cinegrade.infra.config;

/**
 * Singleton class to hold system information.
 */
public enum SystemInfo {

    INSTANCE;

    private final String osName = System.getProperty("os.name");
    private final String osVersion = System.getProperty("os.version");
    private final String osArch = System.getProperty("os.arch");

    private final String javaVersion = System.getProperty("java.version");
    private final String javaVendor = System.getProperty("java.vendor");
    private final String javafxVersion = System.getProperty("javafx.version");

    private final int availableProcessors = Runtime.getRuntime().availableProcessors();
    private final long maxMemoryBytes = Runtime.getRuntime().maxMemory();

    private String gpuRenderer;
    private String gpuVendor;
    private String glVersion;

    private String ffmpegVersion;
    private String ffprobeVersion;
    private String exiftoolVersion;
    private String librawVersion;

    public String getOsName() {
        return osName;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public String getOsArch() {
        return osArch;
    }

    public String getJavaVersion() {
        return javaVersion;
    }

    public String getJavaVendor() {
        return javaVendor;
    }

    public String getJavafxVersion() {
        return javafxVersion;
    }

    public int getAvailableProcessors() {
        return availableProcessors;
    }

    public long getMaxMemoryBytes() {
        return maxMemoryBytes;
    }

    public long getMaxMemoryMb() {
        return maxMemoryBytes / (1024 * 1024);
    }

    public String getGpuRenderer() {
        return gpuRenderer;
    }

    public void setGpuRenderer(String gpuRenderer) {
        this.gpuRenderer = gpuRenderer;
    }

    public String getGpuVendor() {
        return gpuVendor;
    }

    public void setGpuVendor(String gpuVendor) {
        this.gpuVendor = gpuVendor;
    }

    public String getGlVersion() {
        return glVersion;
    }

    public void setGlVersion(String glVersion) {
        this.glVersion = glVersion;
    }

    public boolean isGpuInfoAvailable() {
        return gpuRenderer != null;
    }

    public void setExiftoolVersion(String exiftoolVersion) { this.exiftoolVersion = exiftoolVersion; }
    public void setFfmpegVersion(String ffmpegVersion) { this.ffmpegVersion = ffmpegVersion; }
    public void setFfprobeVersion(String ffprobeVersion) { this.ffprobeVersion = ffprobeVersion; }
    public void setLibrawVersion(String librawVersion) { this.librawVersion = librawVersion; }
}
