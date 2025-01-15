package com.github.codes2prompt.util;

import com.intellij.openapi.diagnostic.Logger;

public class PerformanceLogger {
    private static final Logger LOG = Logger.getInstance(PerformanceLogger.class);

    public static void logTime(String operation, long startTime) {
        long elapsed = System.currentTimeMillis() - startTime;
        LOG.info(String.format("%s took %d ms", operation, elapsed));
    }
} 