package cn.com.vortexa.bot_platform.service;

import java.util.List;
import java.util.Map;

/**
 * @author h30069248
 * @since 2025/5/6 14:18
 */
public interface IVersionService {

    /**
     * 获取bot最新的版本
     *
     * @param botNames botNames
     * @return botName -> version
     */
    Map<String, String> queryBotNewestVersions(List<String> botNames);
}
