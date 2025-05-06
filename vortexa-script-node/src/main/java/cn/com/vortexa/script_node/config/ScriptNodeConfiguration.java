package cn.com.vortexa.script_node.config;

import cn.com.vortexa.common.util.ImageBase64Util;
import com.alibaba.fastjson.JSONObject;

import cn.com.vortexa.common.constants.HttpMethod;
import cn.com.vortexa.common.dto.Result;
import cn.com.vortexa.common.dto.config.AutoBotAccountConfig;
import cn.com.vortexa.common.dto.config.AutoBotConfig;
import cn.com.vortexa.common.util.FileUtil;
import cn.com.vortexa.common.util.VersionUtil;
import cn.com.vortexa.common.util.YamlConfigLoadUtil;
import cn.com.vortexa.common.util.http.RestApiClientFactory;
import cn.com.vortexa.common.dto.BotMetaInfo;
import cn.com.vortexa.web3.constants.Web3ChainDict;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.ResponseBody;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
    public static final String BOT_ICON_FILE_NAME = "icon.png";
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
     * 要使用的bot name列表
     */
    private List<String> loadBotNames;

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
     * bot版本   （解析目录自动写入）
     */
    private Map<String, String> botVersionMap;

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

        // 尝试从platform拉取最新jar包
        tryUpdateNewestBotJarFile();

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
     * 获取bot版本的api
     *
     * @return String
     */
    public String buildBotVersionRestApi() {
        return remoteRestUrl + "/version/botVersions";
    }

    /**
     * 尝试更新bot 的jar包
     */
    private void tryUpdateNewestBotJarFile() throws IOException {
        // Step 1 扫描script node jar包目录，获取botName -> version
        botVersionMap = VersionUtil.scanJarLibForBotVersionMap(FileUtil.getJarFileDir());

        // Step 2 从platform获取最新的版本信息
        Map<String, String> newestBotVersion = fetchRemoteNewestBotVersion(buildBotVersionRestApi(),
                new ArrayList<>(botVersionMap.keySet()));
        if (newestBotVersion != null) {
            // Step 3 对比版本，如果有新版本，则下载
            Iterator<Map.Entry<String, String>> iterator = botVersionMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, String> entry = iterator.next();
                String botName = entry.getKey();
                String v1 = entry.getKey();
                String v2 = newestBotVersion.get(botName);
                if (StrUtil.isBlank(v2)) {
                    // 远程没这个版本的bot, 去除后续不使用
                    iterator.remove();
                } else {
                    int compare = VersionUtil.compareVersion(v1, v2);
                    if (compare < 0) {
                        try {
                            downloadNewestBotJarFile(botName, v2);
                            botVersionMap.put(botName, v2);
                        } catch (ExecutionException | InterruptedException e) {
                            log.error("download newest bot[{}] version[{}] jar file error", botName, v2, e);
                            botVersionMap.remove(botName);
                        }
                    }
                }
            }
        }
    }

    /**
     * 下载最新的jar文件
     *
     * @param botName botName
     * @param version version
     */
    private void downloadNewestBotJarFile(String botName, String version)
            throws ExecutionException, InterruptedException, IOException {
        // 本地版本小于远程版本，需要更新
        log.info("botName[{}] local version less than remote version[{}], try update ...", botName, version);
        String downloadUrl = remoteRestUrl + "/version/bot/download" + botName + "/" + version;
        String fileName = VersionUtil.getBotJarFileName(botName, version);

        Response response = RestApiClientFactory.getClient().rawRequest(
                downloadUrl,
                HttpMethod.GET,
                new HashMap<>(),
                null,
                null
        ).get();
        // 保存文件到本地
        File file = new File(FileUtil.getJarFilePath(fileName));
        ResponseBody responseBody = response.body();
        if (responseBody == null) {
            throw new IOException("response body is null");
        }
        try (InputStream in = responseBody.byteStream();
             OutputStream out = Files.newOutputStream(file.toPath())
        ) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            log.info("update bot[{}]-[{}] jar file success, path: {}", botName, version, file.getAbsolutePath());
        }
    }

    /**
     * 获取原创最新的bot版本
     *
     * @param botVersionApi botVersionApi
     * @param botNames      botNames
     * @return Map<String, String>
     */
    private Map<String, String> fetchRemoteNewestBotVersion(String botVersionApi, List<String> botNames) {
        if (StrUtil.isBlank(botVersionApi)) {
            return null;
        }
        try {
            JSONObject body = new JSONObject();
            body.put("botNames", botNames);
            String response = RestApiClientFactory.getClient().request(
                    botVersionApi,
                    HttpMethod.POST,
                    new HashMap<>(),
                    null,
                    body
            ).get();
            Result result = JSONObject.parseObject(response, Result.class);
            if (result.getSuccess()) {
                return JSONObject.parseObject(JSONObject.toJSONString(result.getData()), Map.class);
            } else {
                log.warn("remote newest bot version fetch fail, {}", result.getErrorMsg());
                return new HashMap<>();
            }
        } catch (InterruptedException | ExecutionException e) {
            log.error("remote newest bot version fetch error", e);
            return new HashMap<>();
        }
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
                    // 配置文件校验
                    if (botConfig == null) {
                        throw new IllegalArgumentException(
                                "bot instance config file [" + configFile.getFileName() + "] illegal");
                    }

                    if (botConfig.getCustomConfig() == null) {
                        botConfig.setCustomConfig(new HashMap<>());
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
     * <P>只有有版本信息的才会被加载</P>
     *
     * @throws IOException IOException
     */
    private void initBotMetaInfo() throws IOException {
        botNameMetaInfoMap = new HashMap<>();
        if (CollUtil.isNotEmpty(loadBotNames)) {
            for (String loadBotName : loadBotNames) {
                String version = this.botVersionMap.get(loadBotName);
                if (StrUtil.isBlank(version)) {
                    log.warn("bot[{}] no version, skip load it", loadBotName);
                    continue;
                }
                String botJarFileName = VersionUtil.getBotJarFileName(loadBotName, version)
                        .replace(".jar", "");
                String jarLibraryPath = FileUtil.getLibraryPath(botJarFileName);
                Path jarFilePath = Paths.get(FileUtil.getJarFilePath(botJarFileName));
                if (!Files.exists(jarFilePath)) {
                    FileUtil.extractJar(
                            jarLibraryPath,
                            jarFilePath.toString()
                    );
                } else {
                    log.info("jar[{}] extracted, skip extract it", botJarFileName);
                }


                // 解析文件夹
                log.info("start resolve bot meta info config from dir[{}]", jarFilePath);
                try (Stream<Path> walk = Files.walk(jarFilePath, 5)) {
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

                            try {
                                metaInfo.setIcon(
                                        ImageBase64Util.pngToBase64DataUrl(metaInfo.getResourceDir() + File.separator + BOT_ICON_FILE_NAME)
                                );
                            } catch (IOException e) {
                                log.warn("bot[{}] icon png image load fail, {}", loadBotName, e.getMessage());
                            }
                            metaInfo.setVersion(version);
                            botNameMetaInfoMap.put(metaInfo.getBotName(), metaInfo);
                            log.info("botName[{}]-[{}] meta info loaded", metaInfo.getBotName(), version);
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
