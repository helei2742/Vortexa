package cn.com.vortexa.script_node.config;

import com.alibaba.fastjson.JSONObject;

import cn.com.vortexa.common.constants.HttpMethod;
import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.common.dto.config.AutoBotAccountConfig;
import cn.com.vortexa.common.dto.config.AutoBotConfig;
import cn.com.vortexa.common.util.FileUtil;
import cn.com.vortexa.common.util.YamlConfigLoadUtil;
import cn.com.vortexa.common.util.http.RestApiClientFactory;
import cn.com.vortexa.common.dto.BotMetaInfo;
import cn.com.vortexa.web3.constants.Web3ChainDict;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

/**
 * @author helei
 * @since 2025-04-04
 */
@Slf4j
@Data
@Component
@ConfigurationProperties(prefix = "vortexa.script-node")
public class ScriptNodeConfiguration implements InitializingBean {
    public static Map<String, Object> RAW_CONFIG = null;
    public static final String BOT_META_INF_FILE_NAME = "bot-meta-info.yaml";
    public static final List<String> BOT_META_INFO_PREFIX = List.of("vortexa", "botMetaInfo");
    public static final List<String> BOT_INSTANCE_CONFIG_PREFIX = List.of("vortexa", "botInstance");

    /**
     * bot group（Script Node中运行的bot的group)
     * ）
     */
    private String scriptNodeName;

    /**
     * 远程REST接口的url
     */
    private String remoteRestUrl;

    /**
     * 链信息字典
     */
    private Web3ChainDict chainDict;

    /**
     * bot-instance jar包名字
     */
    private List<String> botInstanceJarNames;

    /**
     * Script node 基础路径
     */
    private String scriptNodeBasePath;

    /**
     * 是否开启命令行菜单
     */
    private boolean commandMenu = true;

    /**
     * botNameMetaInfoMap, （解析配置文件自动填入）
     */
    private Map<String, BotMetaInfo> botNameMetaInfoMap;

    /**
     * botKeyConfigMap, （解析配置文件自动填入）
     */
    private Map<String, AutoBotConfig> botKeyConfigMap;

    /**
     * 自动时自动启动的botKey
     */
    private Set<String> autoLaunchBotKeys;

    /**
     * bot公共配置，会加载到每个bot的 customConfig下
     *
     * @see AutoBotConfig
     */
    private Map<String, Object> botCommonConfig;

    @Override
    public void afterPropertiesSet() throws Exception {
        // 解析地址，
        scriptNodeBasePath = FileUtil.getAppResourceAppConfigDir() + File.separator + scriptNodeName;

        initBotMetaInfo();

        initBotInstance();
    }

    /**
     * 获取远程设置api
     *
     * @return String
     */
    public String buildRemoteConfigRestApi() {
        return remoteRestUrl + "/script-node/remote-config";
    }

    /**
     * 初始化BotInstance
     */
    private void initBotInstance() {
        botKeyConfigMap = new HashMap<>();
        Path botInstanceConfigPath = Paths.get(FileUtil.getBotInstanceConfigDir());
        if (!Files.exists(botInstanceConfigPath) || !Files.isDirectory(botInstanceConfigPath)) {
            log.warn("no bot instance config dir [{}]", botInstanceConfigPath);
            return;
        }

        try (Stream<Path> walk = Files.walk(botInstanceConfigPath, 5)) {
            walk.filter(p -> Files.isRegularFile(p) && p.toString().endsWith(".yaml")).forEach(configFile -> {
                try {
                    AutoBotConfig botConfig = YamlConfigLoadUtil.load(configFile.toFile(), BOT_INSTANCE_CONFIG_PREFIX,
                            AutoBotConfig.class);
                    if (botConfig.getCustomConfig() == null) botConfig.setCustomConfig(new HashMap<>());
                    // 配置文件校验
                    if (botConfig == null) {
                        throw new IllegalArgumentException(
                                "bot instance config file [" + configFile.getFileName() + "] illegal");
                    }

                    BotMetaInfo botMetaInfo = botNameMetaInfoMap.get(botConfig.getBotName());

                    if (botMetaInfo == null) {
                        log.warn("botName[{}] didn't loaded in script node", botConfig.getBotName());
                        return;
                    }

                    botConfig.setMetaInfo(botMetaInfo);

                    // 相对路径转绝对路径
                    reactivePathConfigConvert(
                            botConfig,
                            botMetaInfo.getResourceDir()
                    );

                    // 合并bot公共配置
                    if (botCommonConfig != null) {
                        botConfig.getCustomConfig().putAll(botCommonConfig);
                    }

                    // 合并远程配置
                    AutoBotConfig remoteBotConfig = fetchRemoteBotConfig(
                            buildRemoteConfigRestApi(),
                            scriptNodeName,
                            botConfig.getBotKey()
                    );
                    if (remoteBotConfig != null) {
                        mergeRemoteAutoBotConfig(botConfig, remoteBotConfig);
                    }

                    botKeyConfigMap.put(botConfig.getBotKey(), botConfig);
                } catch (Exception e) {
                    log.error("bot instance config[{}] load error, {}",
                            configFile.getFileName(),
                            e.getCause() == null ? e.getMessage() : e.getCause().getMessage()
                    );
                }
            });
        } catch (IOException e) {
            log.error("load bot instance config error", e);
        }
    }

    /**
     * 初始化bot原信息
     *
     * @throws IOException IOException
     */
    private void initBotMetaInfo() throws IOException {
        botNameMetaInfoMap = new HashMap<>();
        if (CollUtil.isNotEmpty(botInstanceJarNames)) {
            for (String botInstanceJarName : botInstanceJarNames) {
                String jarLibraryPath = FileUtil.getLibraryPath(botInstanceJarName);
                String jarFilePath = FileUtil.getJarFilePath(botInstanceJarName);
                FileUtil.extractJar(
                        jarLibraryPath,
                        jarFilePath
                );

                // 解析文件夹
                log.info("start resolve bot meta info config from dir[{}]", jarFilePath);
                try (Stream<Path> walk = Files.walk(Paths.get(jarFilePath), 5)) {
                    walk.filter(Files::isDirectory).forEach(dir -> {
                        Path configFilePath = dir.resolve(BOT_META_INF_FILE_NAME);
                        if (Files.exists(configFilePath)) {
                            BotMetaInfo metaInfo = YamlConfigLoadUtil.load(configFilePath.toFile(),
                                    BOT_META_INFO_PREFIX, BotMetaInfo.class);

                            // 配置文件校验
                            if (metaInfo == null) {
                                throw new IllegalArgumentException(
                                        "bot meta info file [" + BOT_META_INF_FILE_NAME + "] illegal");
                            }

                            // 设置bot资源目录
                            metaInfo.setResourceDir(dir.toString());
                            // 设置所在jar包路径
                            metaInfo.setClassJarPath(jarLibraryPath);

                            botNameMetaInfoMap.put(metaInfo.getBotName(), metaInfo);
                            log.info("botName [{}] meta info loaded", metaInfo.getBotName());
                        }
                    });
                }
            }
        }
    }

    /**
     * 获取远程配置
     *
     * @param configUrl      configUrl
     * @param scriptNodeName scriptNodeName
     * @param botKey         botKey
     * @return String
     */
    private AutoBotConfig fetchRemoteBotConfig(String configUrl, String scriptNodeName, String botKey) {
        if (StrUtil.isBlank(configUrl)) {
            return null;
        }

        try {
            JSONObject params = new JSONObject();
            params.put("scriptNodeName", scriptNodeName);
            params.put("botKey", botKey);
            String response = RestApiClientFactory.getClient().request(
                    configUrl,
                    HttpMethod.POST,
                    new HashMap<>(),
                    params,
                    new JSONObject()
            ).get();
            Result result = JSONObject.parseObject(response, Result.class);
            if (result.getSuccess()) {
                AutoBotConfig load = YamlConfigLoadUtil.load(String.valueOf(result.getData()),
                        BOT_INSTANCE_CONFIG_PREFIX, AutoBotConfig.class);
                log.info("remote config fetch success, merge into [{}] bot config...", botKey);
                return load;
            } else {
                log.warn("script node[{}] botKey[{}] config not found in remote", scriptNodeName, botKey);
                return null;
            }
        } catch (InterruptedException | ExecutionException e) {
            log.error("script node[{}] botKey[{}] remote fetch error", scriptNodeName, botKey, e);
            return null;
        }
    }

    /**
     * 合并远程配置
     *
     * @param local  local
     * @param remote remote
     */
    private void mergeRemoteAutoBotConfig(AutoBotConfig local, AutoBotConfig remote) {
        if (remote.getAccountConfig() != null) {
            local.setAccountConfig(remote.getAccountConfig());
        }
        if (remote.getCustomConfig() != null) {
            local.setCustomConfig(remote.getCustomConfig());
        }
    }

    /**
     * 相对路径转换
     *
     * @param config          config
     * @param botResourcePath botResourcePath
     */
    private void reactivePathConfigConvert(AutoBotConfig config, String botResourcePath) {
        AutoBotAccountConfig accountConfig = config.getAccountConfig();
        accountConfig.setConfigFilePath(
                FileUtil.generateAbsPath(accountConfig.getConfigFilePath(), botResourcePath)
        );

        Map<String, Object> customConfig = config.getCustomConfig();
        if (customConfig != null && !customConfig.isEmpty()) {
            for (Map.Entry<String, Object> entry : customConfig.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof String) {
                    entry.setValue(FileUtil.generateAbsPath((String) value, botResourcePath));
                }
            }
        }
    }
}
