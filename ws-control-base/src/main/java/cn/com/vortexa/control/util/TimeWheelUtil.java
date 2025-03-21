package cn.com.vortexa.control.util;


import io.netty.util.HashedWheelTimer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author helei
 * @since 2025-03-12
 */
public class TimeWheelUtil {

    private final static ExecutorService taskExecutor;

    static {
        taskExecutor = Executors.newFixedThreadPool(1);
    }

    public static HashedWheelTimer newTimeout() {
        return new HashedWheelTimer(Executors.defaultThreadFactory(), 1000L, TimeUnit.MILLISECONDS, 512, true, -1L, taskExecutor);
    }
}
