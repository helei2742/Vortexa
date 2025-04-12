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

    /**
     * 标识bot，不同于bot id， botKey是由用户定义的
     */
    private String botKey;

    /**
     * 是否开启命令行菜单
     */
    private boolean commandMenu = true;

    /**
     * 资源文件dir
     */
    private String resourceDir;

    /**
     * 类名
     */
    private String className;
    /**
     * class 文件名
     */
    private String classFileName;
    /**
     * class 文件绝对路径
     */
    private String classFilePath;

    /**
     * 其它class文件
     */
    private List<ClassInfo> extraClass;

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
