package cn.com.vortexa.bot_platform.dto;

import cn.com.vortexa.control.constant.WSControlSystemConstants;
import lombok.Data;

/**
 * @author helei
 * @since 2025-03-31
 */
@Data
public class BotJob {
    private String group;
    private String botName;
    private String botKey;
    private String jobName;

    public String getGroup() {
        if (group == null) {
            return WSControlSystemConstants.DEFAULT_GROUP;
        }

        return group;
    }
}
