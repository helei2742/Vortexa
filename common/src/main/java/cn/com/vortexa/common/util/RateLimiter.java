package cn.com.vortexa.common.util;

import javax.naming.LimitExceededException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class RateLimiter {
    private static final int INTERVAL = 500;

    private final int maxRequestsPerSecond;
    private final AtomicLong lastRequestTime;

    public RateLimiter(int maxRequestsPerSecond) {
        this.maxRequestsPerSecond = maxRequestsPerSecond;
        this.lastRequestTime = new AtomicLong(0);
    }

    // 调用该方法会限制频率
    public boolean allowRequest() {
        long currentTime = System.currentTimeMillis();
        long lastTime = lastRequestTime.get();

        // 判断时间是否已过去足够长，允许下一次请求
        if (currentTime - lastTime >= 1000 / maxRequestsPerSecond) {
            // 使用 compareAndSet 确保只有一个线程可以成功更新 lastRequestTime
            return lastRequestTime.compareAndSet(lastTime, currentTime);
        }

        return false; // 限制请求频率
    }

    // 限制频率的包装方法
    public void callMethodWithRateLimit(int waitSeconds) throws LimitExceededException, InterruptedException {
        long startTime = System.currentTimeMillis();
        while (!allowRequest()) {
            if (System.currentTimeMillis() - startTime > waitSeconds * 1000L) {
                throw new LimitExceededException("wait time out of limit: " + waitSeconds + " seconds");
            }
            TimeUnit.MILLISECONDS.sleep(INTERVAL);
        }
    }

    public static void main(String[] args) throws LimitExceededException, InterruptedException {
        // 每秒最多允许 3 次请求
        RateLimiter rateLimiter = new RateLimiter(3);

        // 模拟连续调用方法
        for (int i = 0; i < 10; i++) {
            rateLimiter.callMethodWithRateLimit(10);
            try {
                // 模拟请求间隔
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
