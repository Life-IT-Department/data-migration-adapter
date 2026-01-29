package lk.avengers.datamigrationadapter.util;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for monitoring and logging memory usage
 */
@Slf4j
public class MemoryMonitor {

    private MemoryMonitor() {
        // Private constructor to prevent instantiation
    }

    /**
     * Log current memory usage with context
     *
     * @param context Description of when this is being called
     */
    public static void logMemoryUsage(String context) {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory() / (1024 * 1024); // MB
        long freeMemory = runtime.freeMemory() / (1024 * 1024);   // MB
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory() / (1024 * 1024);     // MB

        log.info("Memory [{}] - Used: {}MB, Free: {}MB, Total: {}MB, Max: {}MB",
                context, usedMemory, freeMemory, totalMemory, maxMemory);

        // Warn if memory usage is high
        double usagePercentage = (double) usedMemory / maxMemory * 100;
        if (usagePercentage > 80) {
            log.warn("⚠️ Memory usage is HIGH: {}% - Consider running garbage collection",
                    String.format("%.2f", usagePercentage));
        }
    }

    /**
     * Get current memory usage percentage
     *
     * @return Memory usage percentage (0-100)
     */
    public static double getMemoryUsagePercentage() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        long maxMemory = runtime.maxMemory() / (1024 * 1024);
        return (double) usedMemory / maxMemory * 100;
    }

    /**
     * Force garbage collection if memory usage is high
     *
     * @param threshold Memory usage percentage threshold (e.g., 80.0 for 80%)
     * @return true if GC was triggered, false otherwise
     */
    public static boolean forceGcIfNeeded(double threshold) {
        double usage = getMemoryUsagePercentage();
        if (usage > threshold) {
            log.warn("Memory usage at {}% exceeds threshold of {}%. Forcing garbage collection...",
                    String.format("%.2f", usage), threshold);
            System.gc();
            try {
                Thread.sleep(500); // Give GC time to run
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            logMemoryUsage("After Forced GC");
            return true;
        }
        return false;
    }

    /**
     * Get available free memory in MB
     *
     * @return Available free memory in megabytes
     */
    public static long getAvailableMemoryMB() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.freeMemory() / (1024 * 1024);
    }

    /**
     * Get maximum available memory in MB
     *
     * @return Maximum available memory in megabytes
     */
    public static long getMaxMemoryMB() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.maxMemory() / (1024 * 1024);
    }

    /**
     * Check if there's enough memory available for an operation
     *
     * @param requiredMemoryMB Required memory in MB
     * @return true if enough memory is available
     */
    public static boolean hasEnoughMemory(long requiredMemoryMB) {
        long availableMemory = getAvailableMemoryMB();
        boolean hasEnough = availableMemory >= requiredMemoryMB;

        if (!hasEnough) {
            log.warn("Insufficient memory: Required {}MB, Available {}MB",
                    requiredMemoryMB, availableMemory);
        }

        return hasEnough;
    }
}
