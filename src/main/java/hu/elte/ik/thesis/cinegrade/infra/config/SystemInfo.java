package hu.elte.ik.thesis.cinegrade.infra.config;

/**
 * Singleton class to hold system information.
 */
public class SystemInfo{

    private static final SystemInfo INSTANCE = new SystemInfo();

    private final String osName;
    private final String osVersion;
    private final String osArch;

    private final String javaVersion;
    private final String javaVendor;
    private final String javafxVersion;

    private final int availableProcessors;
    private final long maxMemoryBytes;

    private String gpuRenderer;
    private String gpuVendor;
    private String glVersion;

    public SystemInfo() {
        this.osName = System.getProperty("os.name");
        this.osVersion = System.getProperty("os.version");
        this.osArch = System.getProperty("os.arch");

        this.javaVersion = System.getProperty("java.version");
        this.javaVendor = System.getProperty("java.vendor");
        this.javafxVersion = System.getProperty("javafx.version");

        this.availableProcessors = Runtime.getRuntime().availableProcessors();
        this.maxMemoryBytes = Runtime.getRuntime().maxMemory();

        // TODO: populate GPU info later by RenderEngine
        this.gpuRenderer = null;
        this.gpuVendor = null;
        this.glVersion = null;
    }

    public static SystemInfo getInstance() {
        return INSTANCE;
    }

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
}
