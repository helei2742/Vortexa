package cn.com.vortexa.script_node.config;

import cn.com.vortexa.common.dto.config.AutoBotAccountConfig;
import cn.com.vortexa.common.dto.config.AutoBotConfig;
import cn.com.vortexa.common.util.FileUtil;
import cn.com.vortexa.common.util.YamlConfigLoadUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
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

    public static final String BOT_INSTANCE_CONFIG_FILE_NAME = "bot-instance-config.yaml";
    public static final List<String> BOT_INSTANCE_CONFIG_PREFIX = List.of("vortexa", "botInstance");

    /**
     * bot group（Script Node中运行的bot的group)
     * ）
     */
    private String scriptNodeName;

    /**
     * bot-instance 配置文件位置
     */
    private String botInstanceLocations;

    /**
     * Script node 基础路径
     */
    private String scriptNodeBasePath;

    /**
     * 解析后的botInstance配置文件绝对路径
     */
    private String resolvedInstanceLocations;

    /**
     * 是否开启命令行菜单
     */
    private boolean commandMenu = true;

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
     * @see AutoBotConfig
     */
    private Map<String, Object> botCommonConfig;

    @Override
    public void afterPropertiesSet() throws Exception {
        botKeyConfigMap = new HashMap<>();
        // 解析地址，
        scriptNodeBasePath = FileUtil.getAppResourceAppConfigPath() + File.separator + scriptNodeName;

        resolvedInstanceLocations = FileUtil.generateAbsPath(botInstanceLocations, null);

        // 解析文件夹
        log.info("start resolve bot instance config from dir[{}]", resolvedInstanceLocations);
        Path botInstanceDirsPath = Paths.get(resolvedInstanceLocations);
        try (Stream<Path> walk = Files.walk(botInstanceDirsPath, 3)) {
            walk.filter(Files::isDirectory).forEach(dir -> {
                Path configFilePath = dir.resolve(BOT_INSTANCE_CONFIG_FILE_NAME);
                if (Files.exists(configFilePath)) {
                    AutoBotConfig config = YamlConfigLoadUtil.load(configFilePath.toFile(), BOT_INSTANCE_CONFIG_PREFIX, AutoBotConfig.class);

                    // 配置文件校验
                    if (config == null) {
                        throw new IllegalArgumentException("bot instance config file [" + BOT_INSTANCE_CONFIG_FILE_NAME + "] illegal");
                    }
                    // 路径转换
                    reactivePathConfigConvert(config, dir.toString());

                    config.setResourceDir(dir.toString());
                    // 填充公共配置
                    if (config.getCustomConfig() == null) {
                        config.setCustomConfig(new HashMap<>(botCommonConfig));
                    } else {
                        config.getCustomConfig().putAll(botCommonConfig);
                    }
                    botKeyConfigMap.put(config.getBotKey(), config);
                    log.info("botKey[{}] config loaded", config.getBotKey());
                }
            });
        }
    }

    /**
     * 相对路径转换
     *
     * @param config          config
     * @param botInstancePath botInstancePath
     */
    private void reactivePathConfigConvert(AutoBotConfig config, String botInstancePath) {
        AutoBotAccountConfig accountConfig = config.getAccountConfig();
        accountConfig.setConfigFilePath(
                FileUtil.generateAbsPath(accountConfig.getConfigFilePath(), botInstancePath)
        );

        Map<String, Object> customConfig = config.getCustomConfig();
        if (customConfig != null && !customConfig.isEmpty()) {
            for (Map.Entry<String, Object> entry : customConfig.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof String) {
                    entry.setValue(FileUtil.generateAbsPath((String) value, botInstancePath));
                }
            }
        }
    }
}
