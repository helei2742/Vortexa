package cn.com.vortexa.script_node.config;


import cn.com.vortexa.script_node.dto.AutoBotAccountConfig;
import cn.com.vortexa.script_node.dto.AutoBotConfigFilePathConfig;
import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Data
@ToString
@Component
@ConfigurationProperties(prefix = "vortexa")
public class AutoBotConfig {

    public static int LOG_CACHE_COUNT = 50; // 日志缓存数量

    /**
     * bot id
     */
    public static Integer BOT_ID;

    /**
     * botName
     */
    public static String BOT_NAME;


    /**
     * 标识bot，不同于bot id， botKey是由用户定义的
     */
    private String botKey;

    /**
     * 是否开启命令行菜单
     */
    private boolean commandMenu = true;

    /**
     * 配置文件配置
     */
    private AutoBotConfigFilePathConfig filePathConfig = new AutoBotConfigFilePathConfig();

    /**
     * 账户配置
     */
    private AutoBotAccountConfig accountConfig = new AutoBotAccountConfig();

    /**
     * 自定义配置
     */
    private Map<String, Object> customConfig = new HashMap<>();


    public String getConfig(String key) {
        return String.valueOf(customConfig.get(key));
    }

    public void setConfig(String key, String value) {
        this.customConfig.put(key, value);
    }

}
