package cn.com.vortexa.common.vo;

import cn.com.vortexa.common.entity.BotInfo;
import cn.com.vortexa.common.entity.BotInstance;
import lombok.Data;

/**
 * @author helei
 * @since 2025/4/2 14:13
 */
@Data
public class BotInstanceVO {

    private BotInfo botInfo;

    /**
     * bot实例
     */
    private BotInstance botInstance;

    /**
     * 正在运行的jobName
     */
    private Object runningJob;
}
