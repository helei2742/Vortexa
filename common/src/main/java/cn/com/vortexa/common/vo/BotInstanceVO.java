package cn.com.vortexa.common.vo;

import cn.com.vortexa.common.dto.job.JobTrigger;
import cn.com.vortexa.common.entity.BotInfo;
import cn.com.vortexa.common.entity.BotInstance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author helei
 * @since 2025/4/2 14:13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BotInstanceVO {

    private BotInfo botInfo;

    /**
     * bot实例
     */
    private BotInstance botInstance;

    /**
     * 正在运行的jobName
     */
    private Map<String, List<JobTrigger>> jobTriggers;

    /**
     * 是否在线
     */
    private Boolean online;

    /**
     * 启动配置
     */
    private Map<String, Object> botLaunchConfig = new HashMap<>();
}
