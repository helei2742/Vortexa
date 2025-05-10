package cn.com.vortexa.bot_platform.service.impl;

import cn.com.vortexa.bot_platform.service.IBotInfoService;
import cn.com.vortexa.bot_platform.service.IVersionService;
import cn.com.vortexa.common.dto.BotMetaInfo;
import cn.com.vortexa.common.dto.job.AutoBotJobParam;
import cn.com.vortexa.common.entity.BotInfo;
import cn.com.vortexa.common.util.FileUtil;
import cn.com.vortexa.common.util.JarFileResolveUtil;
import cn.com.vortexa.common.util.VersionUtil;
import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author helei
 * @since 2025/5/6 14:19
 */
@Slf4j
@Component
public class VersionServiceImpl implements IVersionService, InitializingBean {

    @Autowired
    private IBotInfoService botInfoService;

    /**
     * platform中的bot版本（local version）  TODO 远程提供版本
     */
    private final Map<String, List<String>> platformBotVersionMap = new ConcurrentHashMap<>();

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("start resolve platform exist bot version...");
        platformBotVersionMap.putAll(VersionUtil.scanJarLibForBotVersionMap(FileUtil.getLibraryDir()));
        log.info("resolve platform exist bot version done, platformBotVersionMap:{}", platformBotVersionMap);

        // 1. 扫描jar包，获取bot的meta信息
        List<String> botJarFileNames = platformBotVersionMap.entrySet()
                .stream()
                .map(e -> VersionUtil.getBotJarFileName(e.getKey(), e.getValue().getFirst()))
                .toList();

        Map<String, BotMetaInfo> metaInfoMap = JarFileResolveUtil.tryExtractJarAndResolveBotMetaInfo(botJarFileNames);

        // 2. 保存bot info
        metaInfoMap.forEach((botName, botMetaInfo) -> {
            try {
                BotInfo.BotInfoBuilder builder = BotInfo.builder()
                        .name(botName)
                        .description(botMetaInfo.getDescription())
                        .image(botMetaInfo.getIcon())
                        .versionCode(botMetaInfo.getVersion_code());
                if (CollUtil.isNotEmpty(botMetaInfo.getJobParams())) {
                    Map<String, AutoBotJobParam> jobParamMap = botMetaInfo.getJobParams()
                            .stream()
                            .collect(Collectors.toMap(AutoBotJobParam::getJobName, jobParam -> jobParam));
                    builder.jobParams(jobParamMap);
                }

                botInfoService.insertOrUpdate(builder.build());
            } catch (Exception e) {
                log.error("update bot[{}] info from bot meta info fail", botName, e);
            }
        });
    }

    @Override
    public Map<String, String> queryBotNewestVersions(List<String> botNames) {
        Map<String, String> result = new HashMap<>(botNames.size());
        botNames.forEach(botName -> {
            result.put(botName, platformBotVersionMap.getOrDefault(botName, new ArrayList<>()).getFirst());
        });
        return result;
    }
}
