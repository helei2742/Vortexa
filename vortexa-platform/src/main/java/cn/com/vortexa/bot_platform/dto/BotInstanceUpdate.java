package cn.com.vortexa.bot_platform.dto;


import cn.com.vortexa.common.dto.job.AutoBotJobParam;
import lombok.Data;

import java.util.Map;

/**
 * @author helei
 * @since 2025-05-04
 */
@Data
public class BotInstanceUpdate {
    private String scriptNodeName;
    private String botName;
    private String botKey;
    private AutoBotJobParam botJobParam;
    private Map<String, Object> botLaunchConfig;
}
