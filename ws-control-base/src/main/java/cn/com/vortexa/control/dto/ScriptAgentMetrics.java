package cn.com.vortexa.control.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author helei
 * @since 2025/3/24 16:03
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ScriptAgentMetrics implements Serializable {
    @Serial
    private static final long serialVersionUID = -984579834902383215L;

    private long totalMemory;   // 总内存
    private long freeMemory;    // 可用内存
    private long maxMemory; // 最大可用内存
    private double processCpuLoad;   // 当前进程cpu利用率
    private double systemCpuLoad;   // 系统cpu利用率

}
