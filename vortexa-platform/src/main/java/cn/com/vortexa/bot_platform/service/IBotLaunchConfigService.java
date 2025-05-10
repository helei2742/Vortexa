package cn.com.vortexa.bot_platform.service;

import cn.com.vortexa.bot_platform.entity.BotLaunchConfig;
import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.common.dto.config.AutoBotConfig;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

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
    Result create(BotLaunchConfig botLaunchConfig) throws SQLException;

    /**
     * 查询script node中botKey的启动参数
     *
     * @param scriptNodeName scriptNodeName
     * @param botKey botKey
     * @return AutoBotConfig
     */
    AutoBotConfig queryScriptNodeBotLaunchConfig(String scriptNodeName, String botKey);

    /**
     * 查询script node中botKey的启动参数,不存在则新建
     *
     * @param scriptNodeName scriptNodeName
     * @param botName   botName
     * @param botKey         botKey
     * @return AutoBotConfig
     */
    AutoBotConfig queryOrCreateScriptNodeBotLaunchConfig(String scriptNodeName, String botName, String botKey);

    /**
     * 查询script node所有的bot启动配置
     *
     * @param scriptNodeName scriptNodeName
     * @return List<AutoBotConfig>
     */
    List<AutoBotConfig> queryScriptNodeAllBotLaunchConfig(String scriptNodeName);

    /**
     * 保存bot启动参数
     *
     * @param scriptNodeName    scriptNodeName
     * @param botKey    botKey
     * @param botLaunchConfig   botLaunchConfig
     * @return Result
     */
    Result saveBotLaunchConfig(String scriptNodeName, String botName, String botKey, Map<String, Object> botLaunchConfig);

    int insertOrUpdate(BotLaunchConfig botLaunchConfig);
}
