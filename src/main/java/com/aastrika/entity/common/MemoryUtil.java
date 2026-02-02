package com.aastrika.entity.common;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class MemoryUtil {

    private MemoryUtil() {
    }

    /**
     * Logs current memory usage
     */
    public static void logMemoryUsage(String label) {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();

        log.info("[{}] Memory - Used: {} MB, Free: {} MB, Total: {} MB, Max: {} MB",
                label,
                toMB(usedMemory),
                toMB(freeMemory),
                toMB(totalMemory),
                toMB(maxMemory));
    }

    /**
     * Returns current used memory in bytes
     */
    public static long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    /**
     * Measures memory used by an operation
     */
    public static long measureMemory(Runnable operation) {
        System.gc();
        long before = getUsedMemory();

        operation.run();

        System.gc();
        long after = getUsedMemory();

        return after - before;
    }

    private static long toMB(long bytes) {
        return bytes / (1024 * 1024);
    }
}
