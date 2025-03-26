package cn.com.vortexa.control.service;

import com.sun.management.OperatingSystemMXBean;

import cn.com.vortexa.control.ScriptAgent;
import cn.com.vortexa.control.constant.RemotingCommandCodeConstants;
import cn.com.vortexa.control.constant.RemotingCommandFlagConstants;
import cn.com.vortexa.control.dto.RemotingCommand;
import cn.com.vortexa.control.dto.ScriptAgentMetrics;
import lombok.extern.slf4j.Slf4j;

import java.lang.management.ManagementFactory;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * Script agent 指标上传服务
 *
 * @author helei
 * @since 2025/3/24 15:09
 */
@Slf4j
public class ScriptAgentMetricsUploadService {
    private static final OperatingSystemMXBean osBean = (OperatingSystemMXBean)ManagementFactory.getOperatingSystemMXBean();
    private static final Runtime runtime = Runtime.getRuntime();

    private final ScriptAgent scriptAgent;
    private final ExecutorService executorService;

    public ScriptAgentMetricsUploadService(
            ScriptAgent scriptAgent
    ) {
        this(scriptAgent, Executors.newFixedThreadPool(1));
    }

    public ScriptAgentMetricsUploadService(
            ScriptAgent scriptAgent,
            ExecutorService executorService
    ) {
        this.scriptAgent = scriptAgent;
        this.executorService = executorService;
    }

    public void start() {
        executorService.execute(new MetricsUploadTask(
                scriptAgent.getClientConfig().getMetricUploadIntervalSeconds(),
                this::uploadMetricsLogic
        ));
    }

    /**
     * 上传指标逻辑
     *
     * @param times times
     */
    private CompletableFuture<RemotingCommand> uploadMetricsLogic(Long times) {
        // Step 1 构建请求
        RemotingCommand request = scriptAgent.newRequestCommand(
                RemotingCommandFlagConstants.SCRIPT_AGENT_METRICS_UPLOAD
        );

        // Step 2 设置指标数据 TODO
        ScriptAgentMetrics metrics = ScriptAgentMetrics.builder()
                .totalMemory(runtime.totalMemory())
                .freeMemory(runtime.freeMemory())
                .maxMemory(runtime.maxMemory())
                .processCpuLoad(osBean.getProcessCpuLoad())
                .processCpuLoad(osBean.getCpuLoad())
                .build();

        request.setObjBody(metrics);

        // Step 3 发送请求
        return scriptAgent.sendRequest(request);
    }

    private static class MetricsUploadTask implements Runnable {

        private final int intervalSeconds; // 间隔
        private final Function<Long, CompletableFuture<RemotingCommand>> uploadLogic;
        private final AtomicLong uploadTimes;
        private volatile boolean running = true;

        private MetricsUploadTask(int intervalSeconds, Function<Long, CompletableFuture<RemotingCommand>> uploadLogic) {
            this.intervalSeconds = intervalSeconds;
            this.uploadLogic = uploadLogic;
            this.uploadTimes = new AtomicLong(0);
        }

        @Override
        public void run() {
            while (running) {
                try {
                    TimeUnit.SECONDS.sleep(intervalSeconds);
                } catch (InterruptedException e) {
                    log.warn("script agent metrics upload task interrupted");
                    running = false;
                    continue;
                }
                try {
                    if (uploadLogic != null) {
                        RemotingCommand response = uploadLogic.apply(uploadTimes.incrementAndGet()).get();
                        if (response.getCode() == RemotingCommandCodeConstants.FAIL) {
                            log.error("script agent metrics upload fail, [{}]", response);
                        }
                    }
                } catch (Exception e) {
                    log.error("script agent metrics upload error", e);
                }
            }
        }
    }
}
