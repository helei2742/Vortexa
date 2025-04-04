package cn.com.vortexa.script_node.config;


import cn.com.vortexa.common.dto.config.ClassInfo;
import cn.com.vortexa.common.dto.config.AutoBotConfig;
import cn.com.vortexa.common.util.FileUtil;
import cn.com.vortexa.common.util.YamlConfigLoadUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    public static final String REACTIVE_PATH_PREFIX = "reactive:";

    /**
     * bot group
     */
    private String botGroup;

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
     * botKeyConfigMap
     */
    private Map<String, AutoBotConfig> botKeyConfigMap;

    @Override
    public void afterPropertiesSet() throws Exception {
        botKeyConfigMap = new HashMap<>();
        // 解析地址，
        scriptNodeBasePath = FileUtil.getBotAppConfigPath() + File.separator + botGroup;

        if (botInstanceLocations.startsWith(REACTIVE_PATH_PREFIX)) {
            resolvedInstanceLocations = botInstanceLocations.replaceFirst(REACTIVE_PATH_PREFIX, scriptNodeBasePath);
        } else {
            resolvedInstanceLocations = botInstanceLocations;
        }

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
                        throw new IllegalArgumentException("bot instance config file [" + BOT_INSTANCE_CONFIG_FILE_NAME+"] illegal");
                    }
                    if (StrUtil.isBlank(config.getClassFileName()) && StrUtil.isBlank(config.getClassFilePath())) {
                        throw new IllegalArgumentException("bot instance config file [" + BOT_INSTANCE_CONFIG_FILE_NAME+"] class path didn't exist");
                    }
                    if (StrUtil.isBlank(config.getClassFilePath())) {
                        config.setClassFilePath(dir + File.separator + config.getClassFileName());
                    }

                    List<ClassInfo> extraClass = config.getExtraClass();
                    if (extraClass != null && !extraClass.isEmpty()) {
                        for (ClassInfo classInfo : extraClass) {
                            if (classInfo.getClassFilePath() == null) {
                                classInfo.setClassFilePath(dir + File.separator + classInfo.getClassFileName());
                            }
                        }
                    }

                    config.setResourceDir(dir.toString());
                    botKeyConfigMap.put(config.getBotKey(), config);
                    log.info("botKey[{}] config loaded", config.getBotKey());
                }
            });
        }
    }
}
