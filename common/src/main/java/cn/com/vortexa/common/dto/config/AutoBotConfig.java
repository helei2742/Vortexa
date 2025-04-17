package cn.com.vortexa.common.dto.config;

import lombok.Data;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@ToString
public class AutoBotConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = -3278263726328378273L;

    public static int LOG_CACHE_COUNT = 50; // 日志缓存数量

    private String botName;

    /**
     * 标识bot，不同于bot id， botKey是由用户定义的
     */
    private String botKey;

    /**
     * 是否开启命令行菜单
     */
    private boolean commandMenu = true;

    /**
     * 资源文件dir, 运行时写入，为class文件所在目录(bot-instance-config所在目录)
     */
    private String resourceDir;

    /**
     * 类名， 必填，还需包含包路径
     */
    private String className;

    private List<String> extraClassNameList;

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
