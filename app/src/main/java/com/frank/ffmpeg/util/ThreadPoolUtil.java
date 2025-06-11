package com.frank.ffmpeg.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadPoolUtil {

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    private ThreadPoolUtil() {
        // Private constructor to prevent instantiation
    }

    public static ExecutorService executeSingleThreadPool(Runnable runnable) {
        executor.submit(runnable);
        return executor;
    }
}
