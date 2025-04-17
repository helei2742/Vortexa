package cn.com.vortexa.control_server.service.impl;

import cn.com.vortexa.control.dto.ScriptAgentMetrics;
import cn.com.vortexa.control_server.service.IMetricsService;

/**
 * @author helei
 * @since 2025/3/24 15:59
 */
public class InfluxDBMetricsService implements IMetricsService {
    @Override
    public int saveAgentMetrics(ScriptAgentMetrics agentMetrics) {
        return 1;
    }
}
