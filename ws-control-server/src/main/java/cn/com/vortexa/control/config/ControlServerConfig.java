package cn.com.vortexa.control.config;


import cn.com.vortexa.common.util.YamlConfigLoadUtil;
import cn.com.vortexa.control.dto.ServiceInstance;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ControlServerConfig {

    private static final String RESOURCE_PATH = "control-server-config.yaml";

    private static final String PREFIX_PATH = "vortexa.nameserver";

    private volatile static ControlServerConfig INSTANCE;

    public static ControlServerConfig defaultConfig() throws FileNotFoundException {
        return loadConfig(RESOURCE_PATH, PREFIX_PATH);
    }

    public static ControlServerConfig loadConfig(String fileName, String prefix) throws FileNotFoundException {
        if (INSTANCE == null) {
            synchronized (ControlServerConfig.class) {
                if (INSTANCE == null) {
                    URL resource = ControlServerConfig.class.getClassLoader().getResource(fileName);
                    if (resource != null) {
                        String file = resource.getFile();
                        INSTANCE = YamlConfigLoadUtil.load(
                                new File(file),
                                List.of(prefix.split("\\.")),
                                ControlServerConfig.class
                        );
                    } else {
                        throw new FileNotFoundException("config file not found");
                    }
                }
            }
        }
        return INSTANCE;
    }


    /**
     * netty nio 线程数
     */
    private Integer nioThreadCount = 1;

    /**
     * 服务地址
     */
    private ServiceInstance serviceInstance;

    /**
     * 下线时间
     */
    private Integer serviceOfflineTtl;


}
