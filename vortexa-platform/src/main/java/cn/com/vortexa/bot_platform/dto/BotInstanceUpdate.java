package cn.com.vortexa.bot_platform.dto;


import cn.com.vortexa.common.dto.job.AutoBotJobParam;
import lombok.Data;

/**
 * @author helei
 * @since 2025-05-04
 */
@Data
public class BotInstanceUpdate {
    private String scriptNodeName;
    private String botKey;
    private AutoBotJobParam botJobParam;
    private String botLaunchConfig;
}
