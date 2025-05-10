package cn.com.vortexa.script_node.config;

import cn.com.vortexa.common.dto.config.AutoBotConfig;
import cn.com.vortexa.common.dto.BotMetaInfo;
import cn.com.vortexa.web3.constants.Web3ChainDict;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @author helei
 * @since 2025-04-04
 */
@Slf4j
@Data
@Component
@ConfigurationProperties(prefix = "vortexa.script-node")
public class ScriptNodeConfiguration  {

    public static Map<String, Object> RAW_CONFIG = null;

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
     * botNameMetaInfoMap, bot信息，（解析配置文件自动填入）
     */
    private Map<String, BotMetaInfo> botNameMetaInfoMap;

    /**
     * botKeyConfigMap, bot instance 信息（解析配置文件自动填入）
     */
    private Map<String, AutoBotConfig> botKeyConfigMap;

    /**
     * bot版本  （解析目录自动写入）
     */
    private Map<String, List<String>> botVersionMap;

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
     * 获取bot jar包下载地址
     *
     * @param botName   botName
     * @param version   version
     * @return String
     */
    public String buildBotJarDownloadUrl(String botName, String version) {
        return remoteRestUrl + "/version/bot/download/" + botName + "/" + version;
    }
}
