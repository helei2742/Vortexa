package cn.com.vortexa.control.service;

import cn.com.vortexa.control.dto.ScriptAgentMetrics;

/**
 * @author helei
 * @since 2025/3/24 15:53
 */
public interface IMetricsService {

    /**
     * 保存指标
     *
     * @param agentMetrics agentMetrics
     * @return int
     */
    int saveAgentMetrics(ScriptAgentMetrics agentMetrics);
}
