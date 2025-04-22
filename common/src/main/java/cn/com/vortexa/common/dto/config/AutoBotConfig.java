package cn.com.vortexa.common.dto.config;

import cn.com.vortexa.common.dto.BotMetaInfo;
import lombok.Data;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Data
@ToString
public class AutoBotConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = -3278263726328378273L;

    public static int LOG_CACHE_COUNT = 50; // 日志缓存数量

    /**
     * bot名
     */
    private String botName;

    /**
     * bot实例key， botKey是由用户定义的
     */
    private String botKey;

    /**
     * bot 元信息
     */
    private BotMetaInfo metaInfo;

    /**
     * 账户配置
     */
    private AutoBotAccountConfig accountConfig = new AutoBotAccountConfig();

    /**
     * 自定义配置
     */
    private Map<String, Object> customConfig = new HashMap<>();


    public String getConfig(String key) {
        return customConfig == null ? null : String.valueOf(customConfig.get(key));
    }

    public void setConfig(String key, String value) {
        this.customConfig.put(key, value);
    }
}
