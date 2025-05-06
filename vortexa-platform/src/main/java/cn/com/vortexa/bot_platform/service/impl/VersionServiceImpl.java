package cn.com.vortexa.bot_platform.service.impl;

import cn.com.vortexa.bot_platform.service.IVersionService;
import cn.com.vortexa.common.util.FileUtil;
import cn.com.vortexa.common.util.VersionUtil;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author h30069248
 * @since 2025/5/6 14:19
 */
@Slf4j
@Component
public class VersionServiceImpl implements IVersionService, InitializingBean {

    /**
     * platform中的bot版本（local version）  TODO 远程提供版本
     */
    private final Map<String, String> platformBotVersionMap = new ConcurrentHashMap<>();

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("start resolve platform exist bot version...");
        platformBotVersionMap.putAll(VersionUtil.scanJarLibForBotVersionMap(FileUtil.getLibraryDir()));
        log.info("resolve platform exist bot version done, platformBotVersionMap:{}", platformBotVersionMap);
    }

    @Override
    public Map<String, String> queryBotNewestVersions(List<String> botNames) {
        Map<String, String> result = new HashMap<>(botNames.size());
        botNames.forEach(botName -> {
            result.put(botName, platformBotVersionMap.getOrDefault(botName, null));
        });
        return result;
    }
}
