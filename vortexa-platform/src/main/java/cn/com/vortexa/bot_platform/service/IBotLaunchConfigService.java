package cn.com.vortexa.bot_platform.service;

import cn.com.vortexa.bot_platform.entity.BotLaunchConfig;
import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.common.dto.config.AutoBotConfig;

import java.util.List;

/**
 * @author helei
 * @since 2025/5/8 15:06
 */
public interface IBotLaunchConfigService {

    /**
     * 新建bot启动配置
     *
     * @param botLaunchConfig botLaunchConfig
     * @return Result
     */
    Result create(BotLaunchConfig botLaunchConfig);

    /**
     * 查询script node中botKey的启动参数
     *
     * @param scriptNodeName scriptNodeName
     * @param botKey botKey
     * @return AutoBotConfig
     */
    AutoBotConfig queryScriptNodeBotLaunchConfig(String scriptNodeName, String botKey);

    /**
     * 查询script node所有的bot启动配置
     *
     * @param scriptNodeName scriptNodeName
     * @return List<AutoBotConfig>
     */
    List<AutoBotConfig> queryScriptNodeAllBotLaunchConfig(String scriptNodeName);
}
