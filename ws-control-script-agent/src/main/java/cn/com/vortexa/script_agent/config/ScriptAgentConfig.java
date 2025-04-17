package cn.com.vortexa.script_agent.config;


import cn.com.vortexa.common.util.YamlConfigLoadUtil;
import cn.com.vortexa.common.dto.control.ServiceInstance;
import lombok.Data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

/**
 * @author helei
 * @since 2025-03-15
 */
@Data
public class ScriptAgentConfig {

    private static final String RESOURCE_PATH = "nameserver-client-config.yaml";

    private static final String PREFIX_PATH = "cn.com.vortexa.nameserver.client";
    private static final Logger log = LoggerFactory.getLogger(ScriptAgentConfig.class);

    private volatile static ScriptAgentConfig INSTANCE;

    public static ScriptAgentConfig defaultConfig() throws FileNotFoundException {
        return loadConfig("nameserver-client-config.yaml", PREFIX_PATH);
    }

    public static ScriptAgentConfig loadConfig(String fileName, String prefix) throws FileNotFoundException {
        if (INSTANCE == null) {
            synchronized (ScriptAgentConfig.class) {
                if (INSTANCE == null) {
                    try(InputStream inputStream = ScriptAgentConfig.class.getClassLoader().getResourceAsStream(fileName)) {
                        if (inputStream != null) {
                            INSTANCE = YamlConfigLoadUtil.load(
                                    fileName,
                                    inputStream,
                                    List.of(prefix.split("\\.")),
                                    ScriptAgentConfig.class
                            );
                        } else {
                            throw new FileNotFoundException("config file not found");
                        }
                    } catch (Exception e) {
                        log.error("load script agent config[{}] error", fileName, e);
                    }
                }
            }
        }
        return INSTANCE;
    }

    private String registryCenterUrl;   // 注册中心地址
    private Integer nioThreadCount = 1; // netty nio 线程数
    private ServiceInstance serviceInstance;    // 服务地址
    private Integer serviceOfflineTtl; // 服务被判断为下线时间（ping interval）

    private int metricUploadIntervalSeconds = 15 * 60;  //  指标上报间隔（秒）

    private static String toCamelCase(String name) {
        String[] parts = name.split("-");
        StringBuilder camelCase = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            camelCase.append(parts[i].substring(0, 1).toUpperCase()).append(parts[i].substring(1));
        }
        return camelCase.toString();
    }
}
